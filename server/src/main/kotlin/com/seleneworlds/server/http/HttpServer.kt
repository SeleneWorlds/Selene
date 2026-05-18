package com.seleneworlds.server.http

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.*
import com.seleneworlds.common.bundles.BundleDatabase
import com.seleneworlds.server.bundles.ClientBundleCache
import com.seleneworlds.server.config.ServerConfig
import com.seleneworlds.server.heartbeat.ServerHeartbeat
import com.seleneworlds.server.login.LoginQueue
import com.seleneworlds.server.login.LoginQueueStatus
import com.seleneworlds.server.login.SessionAuthentication
import com.seleneworlds.server.players.PlayerManager
import com.seleneworlds.server.startupTime
import java.nio.file.Paths

data class SeleneUser(val userId: String, val token: String?)

class HttpServer(
    private val config: ServerConfig,
    private val bundleDatabase: BundleDatabase,
    private val clientBundleCache: ClientBundleCache,
    private val queue: LoginQueue,
    private val playerManager: PlayerManager,
    private val sessionAuth: SessionAuthentication,
    private val serverHeartbeat: ServerHeartbeat
) {
    private fun ApplicationCall.authenticatedUser(): SeleneUser {
        return principal<SeleneUser>()
            ?: if (config.insecureMode) {
                SeleneUser("unauthenticated-user", "unauthenticated-user")
            } else {
                throw IllegalStateException("Authenticated route accessed without principal")
            }
    }

    fun start() {
        embeddedServer(Netty, port = config.apiPort) {
            install(Authentication) {
                bearer("broker") {
                    authenticate { tokenCredential ->
                        sessionAuth.parseToken(tokenCredential.token)
                            .fold(
                                ifLeft = { null },
                                ifRight = { SeleneUser(it.userId, tokenCredential.token) }
                            )
                    }
                }
            }
            install(ContentNegotiation) {
                json()
            }
            routing {
                get("/") {
                    call.respond(
                        ServerStatusResponse(
                            type = "selene",
                            id = serverHeartbeat.serverId,
                            name = config.name,
                            status = "running",
                            timestamp = System.currentTimeMillis(),
                            uptime = System.currentTimeMillis() - startupTime,
                            bundles = BundleCountsResponse(
                                totalCount = bundleDatabase.loadedBundles.size,
                                clientCount = bundleDatabase.loadedBundles.count { clientBundleCache.hasClientSide(it.dir) }
                            ),
                            queueSize = queue.queueSize,
                            maxQueueSize = queue.maxQueueSize,
                            currentPlayers = playerManager.players.size,
                            maxPlayers = 100
                        )
                    )
                }
                get("/heartbeat/jwks") {
                    val publicKey = serverHeartbeat.publicKey

                    call.respond(
                        JwksResponse(
                            keys = listOf(
                                JwkKeyResponse(
                                    kty = "RSA",
                                    use = "sig",
                                    alg = "RS256",
                                    kid = serverHeartbeat.serverId,
                                    n = Base64.getUrlEncoder().withoutPadding().encodeToString(publicKey.modulus.toByteArray()),
                                    e = Base64.getUrlEncoder().withoutPadding().encodeToString(publicKey.publicExponent.toByteArray())
                                )
                            )
                        )
                    )
                }
                authenticate("broker", optional = config.insecureMode) {
                    get("/bundles") {
                        val bundles = bundleDatabase.loadedBundles.associateBy { it.manifest.name }
                            .filter { clientBundleCache.hasClientSide(it.value.dir) }
                            .mapValues { (_, value) ->
                                BundleDescriptorResponse(
                                    name = value.manifest.name,
                                    hash = clientBundleCache.getHash(value.dir),
                                    allowSharedCache = value.manifest.name.startsWith("@"),
                                    variants = listOf("clientZip")
                                )
                            }
                        call.respond(bundles)
                    }
                    get("/bundles/{bundleName}/clientZip") {
                        val bundleName = call.parameters["bundleName"] ?: return@get
                        val bundle = bundleDatabase.getBundle(bundleName) ?: return@get
                        val hash = clientBundleCache.getHash(bundle.dir) ?: return@get
                        call.response.header(
                            HttpHeaders.ContentDisposition, ContentDisposition.Attachment.withParameter(
                                ContentDisposition.Parameters.FileName, "${bundle.manifest.name}-$hash.zip"
                            ).toString()
                        )
                        call.respondFile(clientBundleCache.getZipFile(bundle.dir))
                    }
                    get("/bundles/{bundleName}/content/{path...}") {
                        val bundleName = call.parameters["bundleName"] ?: return@get
                        val unsafePath = call.parameters.getAll("path")?.joinToString("/") ?: return@get
                        val normalizedPath = Paths.get(unsafePath).normalize().toString()

                        val bundle = bundleDatabase.getBundle(bundleName)
                        if (bundle == null) {
                            call.respond(HttpStatusCode.NotFound, "Bundle not found")
                            return@get
                        }

                        val assetPath = bundle.dir.resolve(normalizedPath).normalize()
                        if (normalizedPath.contains("/.") || normalizedPath.startsWith(".")) {
                            call.respond(HttpStatusCode.NotFound, "Asset not found")
                            return@get
                        }

                        val commonBaseDir = bundle.dir.resolve("common").normalize()
                        val clientBaseDir = bundle.dir.resolve("client").normalize()
                        if (!assetPath.startsWith(commonBaseDir) && !assetPath.startsWith(clientBaseDir)) {
                            call.respond(HttpStatusCode.NotFound, "Asset not found")
                            return@get
                        }

                        if (!assetPath.exists()) {
                            call.respond(HttpStatusCode.NotFound, "Asset not found")
                            return@get
                        }

                        if (!assetPath.isFile) {
                            call.respond(HttpStatusCode.BadRequest, "Asset not found")
                            return@get
                        }

                        call.response.header(
                            HttpHeaders.ContentDisposition,
                            ContentDisposition.Inline.withParameter(
                                ContentDisposition.Parameters.FileName,
                                assetPath.name
                            ).toString()
                        )

                        call.respondFile(assetPath)
                    }
                    post("/join") {
                        val principal = call.authenticatedUser()
                        val queueStatus = queue.updateUser(principal.userId)
                        val completedLogin = if (queueStatus.status == LoginQueueStatus.Accepted) {
                            queue.completeJoin(principal.token ?: "unauthenticated-user")
                        } else {
                            null
                        }

                        call.respond(
                            JoinResponse(
                                status = queueStatus.status.name,
                                message = queueStatus.message,
                                token = completedLogin?.token
                            )
                        )
                    }
                    post("/leave") {
                        val principal = call.authenticatedUser()
                        queue.removeUser(principal.userId)
                    }
                }
            }
        }.start()
    }
}

@Serializable
private data class ServerStatusResponse(
    val type: String,
    val id: String,
    val name: String,
    val status: String,
    val timestamp: Long,
    val uptime: Long,
    val bundles: BundleCountsResponse,
    val queueSize: Int,
    val maxQueueSize: Int,
    val currentPlayers: Int,
    val maxPlayers: Int
)

@Serializable
private data class BundleCountsResponse(
    @SerialName("total_count")
    val totalCount: Int,
    @SerialName("client_count")
    val clientCount: Int
)

@Serializable
private data class JwksResponse(
    val keys: List<JwkKeyResponse>
)

@Serializable
private data class JwkKeyResponse(
    val kty: String,
    val use: String,
    val alg: String,
    val kid: String,
    val n: String,
    val e: String
)

@Serializable
private data class BundleDescriptorResponse(
    val name: String,
    val hash: String?,
    @SerialName("allow_shared_cache")
    val allowSharedCache: Boolean,
    val variants: List<String>
)

@Serializable
private data class JoinResponse(
    val status: String,
    val message: String?,
    val token: String?
)
