package world.selene.server.login

import party.iroiro.luajava.Lua
import world.selene.common.lua.LuaMetatable
import world.selene.common.lua.LuaMetatableProvider
import world.selene.server.lua.ServerLuaSignals

enum class LoginQueueStatus {
    Pending,
    Accepted,
    Rejected
}

data class LoginQueueEntry(val userId: String, var status: LoginQueueStatus, var message: String?) : LuaMetatableProvider {
    override fun luaMetatable(lua: Lua): LuaMetatable {
        return LoginQueueEntryLuaApi.luaMeta
    }
}

data class CompletedLogin(val token: String)

class LoginQueue(
    private val signals: ServerLuaSignals,
    private val sessionAuth: SessionAuthentication
) {

    private val entries = mutableMapOf<String, LoginQueueEntry>()

    fun updateUser(userId: String): LoginQueueEntry {
        val entry = entries.getOrPut(userId) {
            LoginQueueEntry(userId, LoginQueueStatus.Pending, null)
        }
        if (signals.playerQueued.hasListeners()) {
            signals.playerQueued.emit {
                it.push(entry, Lua.Conversion.NONE)
                1
            }
        } else {
            entry.status = LoginQueueStatus.Accepted
        }
        return entry
    }

    fun completeJoin(userId: String): CompletedLogin {
        return CompletedLogin(sessionAuth.createToken(SessionAuthentication.TokenData(userId)))
    }

    fun removeUser(userId: String) {
        val entry = entries.remove(userId)
        if (entry != null) {
            signals.playerDequeued.emit {
                it.push(entry, Lua.Conversion.NONE)
                1
            }
        }
    }

    val queueSize get() = entries.size
    val maxQueueSize = 100
}
