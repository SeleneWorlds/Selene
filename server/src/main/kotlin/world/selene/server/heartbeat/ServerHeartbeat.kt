package world.selene.server.heartbeat

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.*
import org.slf4j.Logger
import world.selene.common.util.Disposable
import world.selene.server.config.ServerConfig
import world.selene.server.config.SystemConfig
import world.selene.server.player.PlayerManager
import java.security.KeyPairGenerator
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey
import java.time.Duration
import java.time.Instant
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean

class ServerHeartbeat(
    private val config: ServerConfig,
    private val systemConfig: SystemConfig,
    private val httpClient: HttpClient,
    private val playerManager: PlayerManager,
    private val logger: Logger
) : Disposable {

    val serverId = UUID.randomUUID().toString()
    private val running = AtomicBoolean(false)
    private var heartbeatJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val keyPair = generateRSAKeyPair()
    val publicKey: RSAPublicKey = keyPair.public as RSAPublicKey
    private val privateKey: RSAPrivateKey = keyPair.private as RSAPrivateKey
    private val jwtAlgorithm = Algorithm.RSA256(publicKey, privateKey)

    fun start() {
        if (!config.public) {
            logger.info("Server is not public, skipping heartbeat")
            return
        }
        if (systemConfig.heartbeatServer.isBlank()) {
            logger.info("No heartbeat server configured, skipping heartbeat")
            return
        }

        if (running.compareAndSet(false, true)) {
            logger.info("Announcing server with ID: $serverId")
            heartbeatJob = scope.launch {
                while (running.get()) {
                    sendHeartbeat()
                    delay(heartbeatInterval.toMillis())
                }
            }
        }
    }

    fun stop() {
        if (running.compareAndSet(true, false)) {
            heartbeatJob?.cancel()
            heartbeatJob = null
        }
    }

    override fun dispose() {
        stop()
        scope.cancel()
    }

    private suspend fun sendHeartbeat() {
        val now = Instant.now()
        val publicApiUrl =
            config.announcedApi.ifEmpty { "http://${config.announcedHost.ifEmpty { "localhost" }}:${config.apiPort}" }
        val jwtToken = JWT.create()
            .withIssuer(publicApiUrl)
            .withSubject(serverId)
            .withKeyId(serverId)
            .withAudience(systemConfig.heartbeatServer + "/heartbeat")
            .withIssuedAt(now)
            .withExpiresAt(now.plus(heartbeatInterval))
            .sign(jwtAlgorithm)

        val statusBody = mapOf(
            "name" to config.name,
            "address" to config.announcedHost,
            "port" to config.port,
            "apiUrl" to publicApiUrl,
            "currentPlayers" to playerManager.players.size,
            "maxPlayers" to 100
        )

        try {
            val response = httpClient.post(systemConfig.heartbeatServer + "/heartbeat") {
                contentType(ContentType.Application.Json)
                setBody(statusBody)
                bearerAuth(jwtToken)
            }

            if (!response.status.isSuccess()) {
                logger.warn("Heartbeat request failed with status: ${response.status}")
            }
        } catch (e: Exception) {
            logger.warn("Failed to send heartbeat to ${systemConfig.heartbeatServer}: ${e.message}")
        }
    }

    private fun generateRSAKeyPair() = KeyPairGenerator.getInstance("RSA").apply {
        initialize(2048)
    }.generateKeyPair()

    companion object {
        private val heartbeatInterval = Duration.ofSeconds(30)
    }
}
