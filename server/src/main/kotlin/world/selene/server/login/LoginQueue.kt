package world.selene.server.login

import party.iroiro.luajava.Lua
import world.selene.common.lua.LuaMappedMetatable
import world.selene.common.lua.LuaMetatable
import world.selene.common.lua.LuaMetatableProvider
import world.selene.common.lua.checkString
import world.selene.common.lua.checkUserdata
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
        /**
         * Sends a notification message to the user in the login queue.
         * 
         * ```signatures
         * Notify(message: string)
         * ```
         */
        private fun luaNotify(lua: Lua): Int {
            val entry = lua.checkUserdata<LoginQueueEntry>(1)
            entry.message = lua.checkString(2)
            return 0
        }

        /**
         * Accepts the user's login request, allowing them to join the server.
         * 
         * ```signatures
         * Accept()
         * ```
         */
        private fun luaAccept(lua: Lua): Int {
            val entry = lua.checkUserdata<LoginQueueEntry>(1)
            entry.status = LoginQueueStatus.Accepted
            return 0
        }

        /**
         * Rejects the user's login request with a reason message.
         * 
         * ```signatures
         * Reject(message: string)
         * ```
         */
        private fun luaReject(lua: Lua): Int {
            val entry = lua.checkUserdata<LoginQueueEntry>(1)
            entry.status = LoginQueueStatus.Rejected
            entry.message = lua.checkString(2)
            return 0
        }

        val luaMeta = LuaMappedMetatable(LoginQueueEntry::class) {
            callable(::luaNotify)
            callable(::luaAccept)
            callable(::luaReject)
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
}