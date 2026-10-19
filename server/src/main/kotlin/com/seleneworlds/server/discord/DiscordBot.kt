package com.seleneworlds.server.discord

import com.seleneworlds.common.util.Disposable
import com.seleneworlds.server.config.ServerConfig
import com.seleneworlds.server.players.PlayerManager
import kotlinx.coroutines.*
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.entities.Activity
import net.dv8tion.jda.api.requests.GatewayIntent
import org.slf4j.Logger

/** Owns the Discord connection independently of the game loop. */
class DiscordBot(
    private val config: ServerConfig,
    private val playerManager: PlayerManager,
    private val logger: Logger
) : Disposable {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var started = false

    @Synchronized
    fun start() {
        if (started || config.discordBotToken.isBlank()) return
        started = true
        scope.launch {
            // JDA handles reconnects and restores the latest presence after reconnecting.
            val bot = try {
                JDABuilder.createLight(config.discordBotToken.trim(), emptyList<GatewayIntent>())
                    .setEnableShutdownHook(false)
                    .setActivity(Activity.playing(DiscordPresence.text(playerManager.players.size)))
                    .build()
            } catch (_: Exception) {
                // Do not include exceptions here: authentication diagnostics may contain credentials.
                logger.warn("Could not start Discord bot; check discord_bot_token and Discord connectivity")
                return@launch
            }
            try {
                val presence = DiscordPresence { bot.presence.activity = Activity.playing(it) }
                while (isActive) {
                    try {
                        presence.update(playerManager.players.size)
                    } catch (e: CancellationException) {
                        throw e
                    } catch (_: Exception) {
                        logger.warn("Could not update Discord player count; will retry")
                    }
                    delay(15_000)
                }
            } finally {
                bot.shutdownNow()
            }
        }
    }

    @Synchronized
    override fun dispose() {
        scope.cancel()
    }
}

/** Coalesces player churn between updates and avoids sending unchanged presence. */
internal class DiscordPresence(private val publish: (String) -> Unit) {
    private var lastCount: Int? = null

    fun update(count: Int) {
        if (count == lastCount) return
        publish(text(count))
        lastCount = count
    }

    companion object {
        fun text(count: Int) = "$count ${if (count == 1) "player" else "players"} online"
    }
}
