package com.seleneworlds.server.login

import com.seleneworlds.common.threading.MainThreadDispatcher
import com.seleneworlds.server.players.PlayerEvents
import org.slf4j.Logger

enum class LoginQueueStatus {
    Pending,
    Accepted,
    Rejected
}

data class LoginQueueEntry(val userId: String, var status: LoginQueueStatus, var message: String?)

data class CompletedLogin(val token: String)

class LoginQueue(
    private val mainThreadDispatcher: MainThreadDispatcher,
    private val logger: Logger
) {

    private val entries = mutableMapOf<String, LoginQueueEntry>()

    fun updateUser(userId: String): LoginQueueEntry {
        val isNewEntry = !entries.containsKey(userId)
        val entry = entries.getOrPut(userId) {
            LoginQueueEntry(userId, LoginQueueStatus.Pending, null)
        }
        val previousStatus = entry.status
        val previousMessage = entry.message
        if (isNewEntry) {
            logger.info("Join request received for user {}", userId)
        }
        entry.status = mainThreadDispatcher.callOnMainThread {
            PlayerEvents.PlayerQueued.EVENT.invoker().playerQueued(entry)
        }
        if (entry.status != previousStatus || entry.message != previousMessage) {
            when (entry.status) {
                LoginQueueStatus.Pending -> logger.info("Join request pending for user {}", userId)
                LoginQueueStatus.Accepted -> logger.info("Join request accepted for user {}", userId)
                LoginQueueStatus.Rejected -> logger.warn(
                    "Join request rejected for user {}: {}",
                    userId,
                    entry.message ?: "no rejection reason provided"
                )
            }
        }
        return entry
    }

    fun completeJoin(userId: String, token: String): CompletedLogin {
        logger.info("Completing accepted join for user {}", userId)
        return CompletedLogin(token)
    }

    fun removeUser(userId: String) {
        val entry = entries.remove(userId)
        if (entry != null) {
            logger.info("Join request removed for user {} with status {}", userId, entry.status)
            mainThreadDispatcher.runOnMainThread {
                PlayerEvents.PlayerDequeued.EVENT.invoker().playerDequeued(entry)
            }
        }
    }

    val queueSize get() = entries.size
    val maxQueueSize = 100
}
