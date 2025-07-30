package world.selene.server.login

import party.iroiro.luajava.Lua
import world.selene.common.lua.LuaManager
import world.selene.server.lua.ServerLuaSignals

enum class LoginQueueStatus {
    Pending,
    Accepted,
    Rejected
}

data class LoginQueueEntry(val userId: String, var status: LoginQueueStatus, var message: String?) {
    val luaProxy = LoginQueueEntryLuaProxy(this)

    class LoginQueueEntryLuaProxy(private val delegate: LoginQueueEntry) {
        fun Notify(message: String) {
            delegate.message = message
        }

        fun Accept() {
            delegate.status = LoginQueueStatus.Accepted
        }

        fun Reject(message: String) {
            delegate.status = LoginQueueStatus.Rejected
            delegate.message = message
        }
    }
}

data class CompletedLogin(val token: String)

class LoginQueue(
    luaManager: LuaManager,
    private val signals: ServerLuaSignals,
    private val sessionAuth: SessionAuthentication
) {

    private val entries = mutableMapOf<String, LoginQueueEntry>()

    init {
        luaManager.exposeClass(LoginQueueEntry.LoginQueueEntryLuaProxy::class)
    }

    fun updateUser(userId: String): LoginQueueEntry {
        val entry = entries.getOrPut(userId) {
            LoginQueueEntry(userId, LoginQueueStatus.Pending, null)
        }
        if (signals.playerQueued.hasListeners()) {
            signals.playerQueued.emit {
                it.push(entry.luaProxy, Lua.Conversion.NONE)
                1
            }
        } else {
            entry.status = LoginQueueStatus.Accepted
        }
        return entry
    }

    fun completeJoin(userId: String): CompletedLogin {
        return CompletedLogin(sessionAuth.createToken(userId))
    }

    fun removeUser(userId: String) {
        val entry = entries.remove(userId)
        if (entry != null) {
            signals.playerDequeued.emit {
                it.push(entry.luaProxy, Lua.Conversion.NONE)
                1
            }
        }
    }
}