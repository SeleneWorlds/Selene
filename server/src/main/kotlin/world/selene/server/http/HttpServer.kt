package world.selene.server.http

import com.auth0.jwk.JwkProviderBuilder
import io.ktor.http.ContentDisposition
import io.ktor.http.HttpHeaders
import io.ktor.serialization.jackson.jackson
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.jwt
import io.ktor.server.auth.principal
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.response.header
import io.ktor.server.response.respond
import io.ktor.server.response.respondFile
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import world.selene.common.bundles.BundleDatabase
import world.selene.server.login.LoginQueue
import world.selene.server.login.LoginQueueStatus
import world.selene.server.login.SessionAuthentication
import world.selene.server.bundles.ClientBundleCache
import java.net.URI

data class SeleneUser(val userId: String)

class HttpServer(
    private val bundleDatabase: BundleDatabase,
    private val clientBundleCache: ClientBundleCache,
    private val queue: LoginQueue,
    private val sessionAuth: SessionAuthentication
) {
    fun start() {
        embeddedServer(Netty, port = 8080) {
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
                    call.respond(mapOf("status" to "ok"))
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
