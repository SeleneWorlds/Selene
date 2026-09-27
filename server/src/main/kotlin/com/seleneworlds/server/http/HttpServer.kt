package com.seleneworlds.server.http

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.plugins.forwardedheaders.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.http.content.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.slf4j.Logger
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
import com.seleneworlds.server.config.SystemConfig
import com.seleneworlds.server.heartbeat.ServerHeartbeat
import com.seleneworlds.server.login.LoginQueue
import com.seleneworlds.server.login.LoginQueueStatus
import com.seleneworlds.server.login.ClientAuthorization
import com.seleneworlds.server.login.SessionAuthentication
import com.seleneworlds.server.players.PlayerManager
import com.seleneworlds.server.startupTime
import io.ktor.server.plugins.origin
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
    private val clientAssetIndexProvider: ClientAssetIndexProvider,
    private val systemConfig: SystemConfig,
    private val httpClient: HttpClient,
    private val clientAuthorization: ClientAuthorization,
    private val logger: Logger
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
        val proxyHeaders = ProxyHeaders.parse(config.proxyHeaders)

        val applicationEngine = embeddedServer(Netty, port = config.apiPort) {
            when (proxyHeaders) {
                ProxyHeaders.NONE -> Unit
                ProxyHeaders.FORWARDED -> install(ForwardedHeaders)
                ProxyHeaders.X_FORWARDED -> install(XForwardedHeaders)
            }
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
            val corsOrigins = config.apiCorsOrigins.map { it.trim() }.filter { it.isNotEmpty() }
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
                get("/authorize") {
                    val requestParameters = call.request.queryParameters
                    val clientState = requestParameters["state"]
                    val redirectUri = requestParameters["redirect_uri"]
                    val codeChallenge = requestParameters["code_challenge"]
                    val publicOrigin = call.publicServerOrigin(config.announcedApi, proxyHeaders != ProxyHeaders.NONE)
                    if (requestParameters["response_type"] != "code" || clientState.isNullOrBlank() ||
                        redirectUri.isNullOrBlank() || codeChallenge.isNullOrBlank() ||
                        requestParameters["code_challenge_method"] != "S256" ||
                        !isAllowedClientRedirect(redirectUri, publicOrigin)) {
                        call.respond(HttpStatusCode.BadRequest, "Invalid authorization request.")
                        return@get
                    }

                    val flow = clientAuthorization.begin(clientState, redirectUri, codeChallenge)
                    val callbackUrl = "$publicOrigin/oauth/broker/callback"
                    val brokerUrl = URLBuilder(systemConfig.authBrokerUrl.trimEnd('/') + "/authorize").apply {
                        parameters.append("response_type", "code")
                        parameters.append("redirect_uri", callbackUrl)
                        parameters.append("state", flow.brokerState)
                        parameters.append("code_challenge", ClientAuthorization.sha256UrlSafe(flow.brokerCodeVerifier))
                        parameters.append("code_challenge_method", "S256")
                    }.buildString()
                    call.respondRedirect(brokerUrl)
                }
                get("/oauth/broker/callback") {
                    val code = call.request.queryParameters["code"]
                    val state = call.request.queryParameters["state"]
                    val flow = state?.let(clientAuthorization::consumeBrokerFlow)
                    if (code.isNullOrBlank() || flow == null) {
                        call.respond(HttpStatusCode.BadRequest, "Invalid or expired OAuth callback.")
                        return@get
                    }

                    val publicOrigin = call.publicServerOrigin(config.announcedApi, proxyHeaders != ProxyHeaders.NONE)
                    val callbackUrl = "$publicOrigin/oauth/broker/callback"
                    val response = httpClient.post(systemConfig.authBrokerUrl.trimEnd('/') + "/authorize/token") {
                        setBody(FormDataContent(Parameters.build {
                            append("grant_type", "authorization_code")
                            append("code", code)
                            append("code_verifier", flow.brokerCodeVerifier)
                            append("redirect_uri", callbackUrl)
                        }))
                    }
                    if (!response.status.isSuccess()) {
                        logger.warn(
                            "Authentication broker rejected authorization-code exchange with status {} (broker host: {})",
                            response.status,
                            runCatching { URI(systemConfig.authBrokerUrl).host }.getOrNull() ?: "invalid URL"
                        )
                        call.respond(HttpStatusCode.Unauthorized, "The authentication broker rejected the authorization code.")
                        return@get
                    }
                    val token = response.body<BrokerCodeExchangeResponse>().accessToken.trim()
                    val brokerIdentity = token.takeIf(String::isNotEmpty)?.let { sessionAuth.parseBrokerToken(it, publicOrigin).getOrNull() }
                    if (brokerIdentity == null) {
                        logger.warn("Authentication broker returned an invalid credential for audience {}", publicOrigin)
                        call.respond(HttpStatusCode.Unauthorized, "The authentication broker returned an invalid credential.")
                        return@get
                    }

                    val clientCode = clientAuthorization.issueCode(flow, brokerIdentity.userId)
                    val destination = URLBuilder(flow.clientRedirectUri).apply {
                        parameters.append("code", clientCode)
                        parameters.append("state", flow.clientState)
                    }.buildString()
                    call.respondRedirect(destination)
                }
                post("/token") {
                    val request = call.receiveParameters()
                    if (request["grant_type"] != "authorization_code") {
                        call.respond(HttpStatusCode.BadRequest, "Unsupported grant type.")
                        return@post
                    }
                    val code = request["code"] ?: ""
                    val verifier = request["code_verifier"] ?: ""
                    val redirectUri = request["redirect_uri"] ?: ""
                    val session = clientAuthorization.exchangeCode(code, verifier, redirectUri)
                    if (session == null) {
                        logger.warn(
                            "Rejected client authorization-code exchange from {} (redirect URI: {}, code present: {}, verifier present: {})",
                            call.request.origin.remoteHost,
                            redirectUri.take(200),
                            code.isNotBlank(),
                            verifier.isNotBlank()
                        )
                        call.respond(HttpStatusCode.Unauthorized, "Invalid or expired authorization code.")
                        return@post
                    }
                    call.response.header(HttpHeaders.CacheControl, "no-store")
                    call.response.header(HttpHeaders.Pragma, "no-cache")
                    call.respond(ClientTokenResponse(session.first))
                }
                get("/status") {
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

private fun ApplicationCall.publicServerOrigin(announcedApi: String, trustProxyHeaders: Boolean): String {
    announcedApi.trim().takeIf(String::isNotEmpty)?.let { return it.trimEnd('/') }
    if (!trustProxyHeaders) return "${request.local.scheme}://${request.host()}"
    val origin = request.origin
    return URLBuilder(
        protocol = URLProtocol.createOrDefault(origin.scheme),
        host = origin.serverHost,
        port = origin.serverPort
    ).buildString().trimEnd('/')
}

private enum class ProxyHeaders {
    NONE,
    FORWARDED,
    X_FORWARDED;

    companion object {
        fun parse(value: String): ProxyHeaders = when (value.trim().lowercase()) {
            "none" -> NONE
            "forwarded" -> FORWARDED
            "x-forwarded" -> X_FORWARDED
            else -> throw IllegalArgumentException(
                "Invalid proxy_headers value '$value'; expected none, forwarded, or x-forwarded."
            )
        }
    }
}

private fun isAllowedClientRedirect(rawRedirectUri: String, publicOrigin: String): Boolean {
    if (rawRedirectUri == "selene://auth") return true
    val redirect = runCatching { URI(rawRedirectUri) }.getOrNull() ?: return false
    val origin = runCatching { URI(publicOrigin) }.getOrNull() ?: return false
    return redirect.scheme == origin.scheme && redirect.authority == origin.authority
}

@Serializable
private data class BrokerCodeExchangeResponse(@SerialName("access_token") val accessToken: String)

@Serializable
private data class ClientTokenResponse(
    @SerialName("access_token") val accessToken: String,
    @SerialName("token_type") val tokenType: String = "Bearer",
    @SerialName("expires_in") val expiresIn: Int = 300
)

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
