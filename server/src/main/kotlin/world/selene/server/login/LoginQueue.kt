package world.selene.server.login

import party.iroiro.luajava.Lua
import world.selene.common.lua.LuaMappedMetatable
import world.selene.common.lua.LuaMetatable
import world.selene.common.lua.LuaMetatableProvider
import world.selene.common.lua.checkString
import world.selene.server.lua.ServerLuaSignals

enum class LoginQueueStatus {
    Pending,
    Accepted,
    Rejected
}

data class LoginQueueEntry(val userId: String, var status: LoginQueueStatus, var message: String?) :
    LuaMetatableProvider {

    override fun luaMetatable(lua: Lua): LuaMetatable {
        return luaMeta
    }

    companion object {
        val luaMeta = LuaMappedMetatable(LoginQueueEntry::class) {
            callable("Notify") {
                val entry = it.checkSelf()
                entry.message = it.checkString(1)
                0
            }
            callable("Accept") {
                val entry = it.checkSelf()
                entry.status = LoginQueueStatus.Accepted
                0
            }
            callable("Reject") {
                val entry = it.checkSelf()
                entry.status = LoginQueueStatus.Rejected
                entry.message = it.checkString(1)
                0
            }
        }
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
        return CompletedLogin(sessionAuth.createToken(userId))
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
}