package world.selene.server.heartbeat

import com.fasterxml.jackson.databind.ObjectMapper
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.util.generateNonce
import kotlinx.coroutines.*
import org.slf4j.Logger
import world.selene.common.util.Disposable
import world.selene.server.config.ServerConfig
import world.selene.server.player.PlayerManager
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean

class ServerHeartbeat(
    private val config: ServerConfig,
    private val httpClient: HttpClient,
    private val playerManager: PlayerManager,
    private val objectMapper: ObjectMapper,
    private val logger: Logger
) : Disposable {
    
    val serverId = UUID.randomUUID().toString()
    private val running = AtomicBoolean(false)
    private var heartbeatJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    fun start() {
        if (config.heartbeatServer.isBlank()) {
            logger.info("No heartbeat server configured, skipping heartbeat")
            return
        }
        
        if (running.compareAndSet(false, true)) {
            logger.info("Announcing server with ID: $serverId")
            heartbeatJob = scope.launch {
                while (running.get()) {
                    sendHeartbeat()
                    delay(HEARTBEAT_INTERVAL_MS)
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
        val heartbeatData = HeartbeatData(
            id = serverId,
            port = config.port,
            nonce = generateNonce(),
            timestamp = System.currentTimeMillis(),
            name = config.name,
            currentPlayers = playerManager.players.size,
            maxPlayers = 100
        )
        
        try {
            val response = httpClient.post(config.heartbeatServer + "/heartbeat") {
                contentType(ContentType.Application.Json)
                setBody(objectMapper.writeValueAsString(heartbeatData))
            }
            
            if (!response.status.isSuccess()) {
                logger.warn("Heartbeat request failed with status: ${response.status}")
            }
        } catch (e: Exception) {
            logger.warn("Failed to send heartbeat to ${config.heartbeatServer}: ${e.message}")
        }
    }

    data class HeartbeatData(
        val id: String,
        val name: String,
        val port: Int,
        val timestamp: Long,
        val nonce: String,
        val currentPlayers: Int,
        val maxPlayers: Int
    )

    companion object {
        private const val HEARTBEAT_INTERVAL_MS = 30_000L
    }
}
