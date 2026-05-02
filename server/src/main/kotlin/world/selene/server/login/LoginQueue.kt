package world.selene.server.login

import world.selene.server.players.PlayerEvents

enum class LoginQueueStatus {
    Pending,
    Accepted,
    Rejected
}

data class LoginQueueEntry(val userId: String, var status: LoginQueueStatus, var message: String?)

data class CompletedLogin(val token: String)

class LoginQueue(
    private val sessionAuth: SessionAuthentication
) {

    private val entries = mutableMapOf<String, LoginQueueEntry>()

    fun updateUser(userId: String): LoginQueueEntry {
        val entry = entries.getOrPut(userId) {
            LoginQueueEntry(userId, LoginQueueStatus.Pending, null)
        }
        entry.status = PlayerEvents.PlayerQueued.EVENT.invoker().playerQueued(entry)
        return entry
    }

    fun completeJoin(userId: String): CompletedLogin {
        return CompletedLogin(sessionAuth.createToken(SessionAuthentication.TokenData(userId)))
    }

    fun removeUser(userId: String) {
        val entry = entries.remove(userId)
        if (entry != null) {
            PlayerEvents.PlayerDequeued.EVENT.invoker().playerDequeued(entry)
        }
    }

    val queueSize get() = entries.size
    val maxQueueSize = 100
}
