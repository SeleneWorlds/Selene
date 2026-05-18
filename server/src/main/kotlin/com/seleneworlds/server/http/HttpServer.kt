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
                    call.respond(mapOf(
                        "type" to "selene",
                        "id" to serverHeartbeat.serverId,
                        "name" to config.name,
                        "status" to "running",
                        "timestamp" to System.currentTimeMillis(),
                        "uptime" to System.currentTimeMillis() - startupTime,
                        "bundles" to mapOf(
                            "total_count" to bundleDatabase.loadedBundles.size,
                            "client_count" to bundleDatabase.loadedBundles.count { clientBundleCache.hasClientSide(it.dir) }
                        ),
                        "queueSize" to queue.queueSize,
                        "maxQueueSize" to queue.maxQueueSize,
                        "currentPlayers" to playerManager.players.size,
                        "maxPlayers" to 100
                    ))
                }
                get("/heartbeat/jwks") {
                    val publicKey = serverHeartbeat.publicKey

                    call.respond(mapOf(
                        "keys" to listOf(
                            mapOf(
                                "kty" to "RSA",
                                "use" to "sig",
                                "alg" to "RS256",
                                "kid" to serverHeartbeat.serverId,
                                "n" to Base64.getUrlEncoder().withoutPadding().encodeToString(publicKey.modulus.toByteArray()),
                                "e" to Base64.getUrlEncoder().withoutPadding().encodeToString(publicKey.publicExponent.toByteArray())
                            )
                        )
                    ))
                }
                authenticate("broker", optional = config.insecureMode) {
                    get("/bundles") {
                        val bundles = bundleDatabase.loadedBundles.associateBy { it.manifest.name }
                            .filter { clientBundleCache.hasClientSide(it.value.dir) }
                            .mapValues { (_, value) ->
                                mapOf(
                                    "name" to value.manifest.name,
                                    "hash" to clientBundleCache.getHash(value.dir),
                                    "allow_shared_cache" to value.manifest.name.startsWith("@"),
                                    "variants" to listOf("clientZip")
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

                        call.respond(mapOf(
                            "status" to queueStatus.status.name,
                            "message" to queueStatus.message,
                            "token" to completedLogin?.token
                        ))
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
