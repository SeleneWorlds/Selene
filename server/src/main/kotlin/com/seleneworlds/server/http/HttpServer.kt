package com.seleneworlds.server.http

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.http.content.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.*
import com.seleneworlds.common.bundles.BundleDatabase
import com.seleneworlds.common.bundles.BundleManifest
import com.seleneworlds.common.data.Identifier
import com.seleneworlds.common.serialization.seleneJson
import com.seleneworlds.common.util.Disposable
import com.seleneworlds.server.bundles.ClientBundleCache
import com.seleneworlds.server.bundles.ClientLuaModules
import com.seleneworlds.server.bundles.ClientUiAssets
import com.seleneworlds.server.bundles.ClientRegistrySnapshots
import com.seleneworlds.server.config.ServerConfig
import com.seleneworlds.server.heartbeat.ServerHeartbeat
import com.seleneworlds.server.login.LoginQueue
import com.seleneworlds.server.login.LoginQueueStatus
import com.seleneworlds.server.login.SessionAuthentication
import com.seleneworlds.server.players.PlayerManager
import com.seleneworlds.server.startupTime
import java.net.URI
import java.nio.file.Paths

data class SeleneUser(val userId: String, val token: String?)

class HttpServer(
    private val config: ServerConfig,
    private val bundleDatabase: BundleDatabase,
    private val clientBundleCache: ClientBundleCache,
    private val queue: LoginQueue,
    private val playerManager: PlayerManager,
    private val sessionAuth: SessionAuthentication,
    private val serverHeartbeat: ServerHeartbeat,
    private val clientAssetIndexProvider: ClientAssetIndexProvider
) : Disposable {
    private var engine: EmbeddedServer<NettyApplicationEngine, NettyApplicationEngine.Configuration>? = null
    private val clientLuaModules = ClientLuaModules(bundleDatabase, clientBundleCache)
    private val clientUiAssets = ClientUiAssets(bundleDatabase, clientBundleCache)
    private val clientRegistrySnapshots = ClientRegistrySnapshots(bundleDatabase, clientBundleCache, seleneJson)

    private fun ApplicationCall.authenticatedUser(): SeleneUser {
        return principal<SeleneUser>()
            ?: if (config.insecureMode) {
                SeleneUser("unauthenticated-user", "unauthenticated-user")
            } else {
                throw IllegalStateException("Authenticated route accessed without principal")
            }
    }

    fun start() {
        clientAssetIndexProvider.rebuild()

        val applicationEngine = embeddedServer(Netty, port = config.apiPort) {
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
                json(seleneJson)
            }
            val corsOrigins = config.managementCorsOrigins.map { it.trim() }.filter { it.isNotEmpty() }
            if (corsOrigins.isNotEmpty()) {
                install(CORS) {
                    if ("*" in corsOrigins) {
                        anyHost()
                    } else {
                        corsOrigins.forEach { origin ->
                            if ("://" in origin) {
                                val uri = URI(origin)
                                allowHost(uri.authority, schemes = listOf(uri.scheme))
                            } else {
                                allowHost(origin)
                            }
                        }
                    }
                    allowMethod(HttpMethod.Get)
                    allowMethod(HttpMethod.Post)
                    allowMethod(HttpMethod.Options)
                    allowHeader(HttpHeaders.Authorization)
                    allowHeader(HttpHeaders.ContentType)
                }
            }
            routing {
                post("/bootstrap") {
                    val contentLength = call.request.header(HttpHeaders.ContentLength)?.toLongOrNull()
                    if (contentLength != null && contentLength > MAX_BOOTSTRAP_BODY_BYTES) {
                        call.respond(HttpStatusCode.PayloadTooLarge, "Bootstrap request body is too large.")
                        return@post
                    }

                    val token = when {
                        call.request.contentType().match(ContentType.Application.Json) ->
                            call.receive<BootstrapRequest>().token
                        else -> call.receiveParameters()["token"]
                    }?.trim()

                    if (token.isNullOrEmpty()) {
                        call.respond(HttpStatusCode.BadRequest, "A non-empty token field is required.")
                        return@post
                    }

                    val forwardedProtocol = call.request.header(HttpHeaders.XForwardedProto)
                        ?.substringBefore(',')
                        ?.trim()
                    val secure = forwardedProtocol.equals("https", ignoreCase = true)
                        || call.request.local.scheme.equals("https", ignoreCase = true)
                    call.response.cookies.append(
                        Cookie(
                            name = JOIN_TOKEN_COOKIE_NAME,
                            value = token,
                            path = "/",
                            secure = secure,
                            httpOnly = false,
                            extensions = mapOf("SameSite" to "Strict")
                        )
                    )
                    call.response.header(HttpHeaders.Location, "/")
                    call.respond(HttpStatusCode.SeeOther)
                }
                get("/status") {
                    call.response.header(HttpHeaders.AccessControlAllowOrigin, "*")
                    call.respond(
                        ServerStatusResponse(
                            type = "selene",
                            id = serverHeartbeat.serverId,
                            name = config.name,
                            status = "running",
                            host = config.announcedHost.ifEmpty { null },
                            port = config.port,
                            timestamp = System.currentTimeMillis(),
                            uptime = System.currentTimeMillis() - startupTime,
                            bundles = BundleCountsResponse(
                                totalCount = bundleDatabase.enabledBundles.size,
                                clientCount = bundleDatabase.enabledBundles.count { clientBundleCache.hasClientSide(it.dir) }
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
                get("/client/config") {
                    call.respond(
                        WebClientConfigResponse(
                            webSocketUrl = config.announcedWebSocket.ifBlank { null },
                            webSocketPort = config.webSocketPort
                        )
                    )
                }
                get("/client/ui/content/{bundleName}/{id}/{path...}") {
                    val bundleName = call.parameters["bundleName"] ?: return@get
                    val id = call.parameters["id"] ?: return@get
                    val path = call.parameters.getAll("path")?.joinToString("/") ?: return@get
                    val file = clientUiAssets.resolve(bundleName, id, path)
                    if (file == null) {
                        call.respond(HttpStatusCode.NotFound, "UI asset not found")
                        return@get
                    }
                    call.response.header(HttpHeaders.CacheControl, "no-cache")
                    call.respondFile(file)
                }
                authenticate("broker", optional = config.insecureMode) {
                    get("/bundles") {
                        val bundles = bundleDatabase.enabledBundles.associateBy { it.manifest.name }
                            .filter { clientBundleCache.hasClientSide(it.value.dir) }
                            .mapValues { (_, value) ->
                                BundleDescriptorResponse(
                                    name = value.manifest.name,
                                    manifest = value.manifest,
                                    hash = clientBundleCache.getHash(value.dir),
                                    allowSharedCache = value.manifest.name.startsWith("@"),
                                    variants = listOf("clientZip", "content")
                                )
                            }
                        call.respond(bundles)
                    }
                    get("/bundles/{bundleName}/clientZip") {
                        val bundleName = call.parameters["bundleName"] ?: return@get
                        val bundle = bundleDatabase.getEnabledBundle(bundleName) ?: return@get
                        val hash = clientBundleCache.getHash(bundle.dir) ?: return@get
                        call.response.header(
                            HttpHeaders.ContentDisposition, ContentDisposition.Attachment.withParameter(
                                ContentDisposition.Parameters.FileName, "${bundle.manifest.name}-$hash.zip"
                            ).toString()
                        )
                        call.respondFile(clientBundleCache.getZipFile(bundle.dir))
                    }
                    get("/bundles/{bundleName}/asset-manifest.json") {
                        val bundleName = call.parameters["bundleName"] ?: return@get
                        if (bundleDatabase.getEnabledBundle(bundleName) == null) {
                            call.respond(HttpStatusCode.NotFound, "Bundle not found")
                            return@get
                        }

                        val manifest = clientAssetIndexProvider.current().bundleManifest(bundleName)
                        if (manifest == null) {
                            call.respond(HttpStatusCode.NotFound, "Bundle asset manifest not found")
                            return@get
                        }

                        call.respondAssetManifest(manifest)
                    }
                    get("/bundles/{bundleName}/content/{path...}") {
                        val bundleName = call.parameters["bundleName"] ?: return@get
                        val unsafePath = call.parameters.getAll("path")?.joinToString("/") ?: return@get
                        val normalizedPath = Paths.get(unsafePath).normalize().toString()

                        if (bundleDatabase.getEnabledBundle(bundleName) == null) {
                            call.respond(HttpStatusCode.NotFound, "Bundle not found")
                            return@get
                        }
                        if (normalizedPath.contains("/.") || normalizedPath.startsWith(".")) {
                            call.respond(HttpStatusCode.NotFound, "Asset not found")
                            return@get
                        }

                        val assetIndex = clientAssetIndexProvider.current()
                        val hashedAsset = assetIndex.resolveBundle(bundleName, normalizedPath)
                        if (hashedAsset != null) {
                            call.respondHashedAsset(assetIndex, hashedAsset)
                            return@get
                        }

                        call.respond(HttpStatusCode.NotFound, "Asset not found")
                    }
                    get("/client/asset-manifest.json") {
                        val assetIndex = clientAssetIndexProvider.current()
                        call.respondAssetManifest(VersionedAssetManifest(assetIndex.manifest, assetIndex.manifestEtag))
                    }
                    get("/client/content/{path...}") {
                        val unsafePath = call.parameters.getAll("path")?.joinToString("/") ?: return@get
                        val normalizedPath = Paths.get(unsafePath).normalize().toString()
                        if (normalizedPath.contains("/.") || normalizedPath.startsWith(".")) {
                            call.respond(HttpStatusCode.NotFound, "Asset not found")
                            return@get
                        }

                        val assetIndex = clientAssetIndexProvider.current()
                        val hashedAsset = assetIndex.resolveClient(normalizedPath)
                        if (hashedAsset != null) {
                            call.respondHashedAsset(assetIndex, hashedAsset)
                            return@get
                        }

                        call.respond(HttpStatusCode.NotFound, "Asset not found")
                    }
                    get("/client/registries") {
                        call.respond(clientRegistrySnapshots.getIndex())
                    }
                    get("/client/registries/{registryName...}") {
                        val registryName = call.parameters.getAll("registryName")?.joinToString("/") ?: return@get
                        val snapshot = clientRegistrySnapshots.getRegistry(Identifier.parse(registryName))
                        if (snapshot == null) {
                            call.respond(HttpStatusCode.NotFound, "Registry not found")
                            return@get
                        }

                        call.respond(snapshot)
                    }
                    get("/client/lua") {
                        call.respond(clientLuaModules.getIndex())
                    }
                    get("/client/lua/modules/{moduleName...}") {
                        val moduleName = call.parameters.getAll("moduleName")?.joinToString("/") ?: return@get
                        val module = clientLuaModules.getModule(moduleName)
                        if (module == null) {
                            call.respond(HttpStatusCode.NotFound, "Lua module not found")
                            return@get
                        }

                        call.respond(module)
                    }
                    get("/client/ui") {
                        call.respond(clientUiAssets.getIndex())
                    }
                    post("/join") {
                        val principal = call.authenticatedUser()
                        val queueStatus = queue.updateUser(principal.userId)
                        val completedLogin = if (queueStatus.status == LoginQueueStatus.Accepted) {
                            queue.completeJoin(principal.userId, principal.token ?: "unauthenticated-user")
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
                staticResources("/", "web-client", index = "index.html")
            }
        }

        engine = applicationEngine
        applicationEngine.start()
    }

    override fun dispose() {
        engine?.stop(1_000, 5_000)
        engine = null
    }
}

private const val JOIN_TOKEN_COOKIE_NAME = "selene_join_token"
private const val MAX_BOOTSTRAP_BODY_BYTES = 64 * 1024L

@Serializable
private data class BootstrapRequest(val token: String? = null)

@Serializable
private data class WebClientConfigResponse(
    val webSocketUrl: String?,
    val webSocketPort: Int
)

private suspend fun ApplicationCall.respondAssetManifest(manifest: VersionedAssetManifest) {
    response.header(HttpHeaders.CacheControl, ClientAssetIndex.MANIFEST_CACHE_CONTROL)
    response.header(HttpHeaders.ETag, manifest.etag)

    val requestEtags = request.header(HttpHeaders.IfNoneMatch)
        ?.split(",")
        ?.map { it.trim() }
        .orEmpty()
    if (manifest.etag in requestEtags || "*" in requestEtags) {
        respond(HttpStatusCode.NotModified)
        return
    }

    respond(manifest.manifest)
}

private suspend fun ApplicationCall.respondHashedAsset(
    clientAssetIndex: ClientAssetIndex,
    asset: HashedClientAsset
) {
    if (!clientAssetIndex.isContentCurrent(asset)) {
        respond(
            HttpStatusCode.NotFound,
            "Outdated asset; newer version is available at ${asset.hashedPath}"
        )
        return
    }

    response.header(HttpHeaders.CacheControl, ClientAssetIndex.IMMUTABLE_CACHE_CONTROL)
    response.header(HttpHeaders.ETag, "\"${asset.fullHash}\"")
    response.header(
        HttpHeaders.ContentDisposition,
        ContentDisposition.Inline.withParameter(
            ContentDisposition.Parameters.FileName,
            asset.hashedPath.substringAfterLast('/')
        ).toString()
    )
    respondFile(asset.file)
}

@Serializable
private data class ServerStatusResponse(
    val type: String,
    val id: String,
    val name: String,
    val status: String,
    val host: String? = null,
    val port: Int,
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
    val manifest: BundleManifest,
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
