package world.selene.server.http

import com.auth0.jwk.JwkProviderBuilder
import io.ktor.http.*
import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.util.*
import world.selene.common.bundles.BundleDatabase
import world.selene.server.bundles.ClientBundleCache
import world.selene.server.config.ServerConfig
import world.selene.server.heartbeat.ServerHeartbeat
import world.selene.server.login.LoginQueue
import world.selene.server.login.LoginQueueStatus
import world.selene.server.login.SessionAuthentication
import world.selene.server.players.PlayerManager
import world.selene.server.startupTime
import java.net.URI
import java.nio.file.Paths

data class SeleneUser(val userId: String)

class HttpServer(
    private val config: ServerConfig,
    private val bundleDatabase: BundleDatabase,
    private val clientBundleCache: ClientBundleCache,
    private val queue: LoginQueue,
    private val playerManager: PlayerManager,
    private val sessionAuth: SessionAuthentication,
    private val serverHeartbeat: ServerHeartbeat
) {
    fun start() {
        embeddedServer(Netty, port = config.apiPort) {
            install(Authentication) {
                jwt("user") {
                    verifier(JwkProviderBuilder(URI("https://id.twelveiterations.com/realms/Selene/protocol/openid-connect/certs").toURL()).build())
                    validate { credential ->
                        val claims = credential.payload
                        SeleneUser(claims.subject)
                    }
                }
                jwt("session") {
                    verifier(sessionAuth.issuer, sessionAuth.audience, sessionAuth.algorithm)
                    validate { credential ->
                        val claims = credential.payload
                        SeleneUser(claims.subject)
                    }
                }
            }
            install(ContentNegotiation) {
                jackson()
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
                authenticate("session") {
                    get("/bundles") {
                        call.respond(bundleDatabase.loadedBundles.associateBy { it.manifest.name }
                            .filter { clientBundleCache.hasClientSide(it.value.dir) }
                            .mapValues { (_, value) ->
                                mapOf(
                                    "name" to value.manifest.name,
                                    "hash" to clientBundleCache.getHash(value.dir),
                                    "allow_shared_cache" to value.manifest.name.startsWith("@"),
                                    "variants" to listOf("clientZip")
                                )
                            })
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
                }
                authenticate("user") {
                    post("/join") {
                        val principal = call.principal<SeleneUser>()!!
                        val userId = principal.userId
                        val queueStatus = queue.updateUser(userId)
                        val completedLogin = if (queueStatus.status == LoginQueueStatus.Accepted) {
                            queue.completeJoin(userId)
                        } else {
                            null
                        }

                        call.respond(
                            mapOf(
                                "status" to queueStatus.status.name,
                                "message" to queueStatus.message,
                                "token" to completedLogin?.token
                            )
                        )
                    }
                    post("/leave") {
                        val principal = call.principal<SeleneUser>()!!
                        val userId = principal.userId
                        queue.removeUser(userId)
                    }
                }
            }
        }.start()
    }
}
