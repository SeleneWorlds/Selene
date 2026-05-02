package com.seleneworlds.server.login

import party.iroiro.luajava.Lua
import com.seleneworlds.common.lua.LuaMappedMetatable
import com.seleneworlds.common.lua.util.checkString
import com.seleneworlds.common.lua.util.checkUserdata

object LoginQueueEntryLuaApi {

    /**
     * Unique identifier for the queued user.
     *
     * ```property
     * UserId: string
     * ```
     */
    private fun luaGetUserId(lua: Lua): Int {
        val entry = lua.checkUserdata<LoginQueueEntry>(1)
        lua.push(entry.userId)
        return 1
    }

    /**
     * Current queue state: `"Pending"`, `"Accepted"`, or `"Rejected"`.
     *
     * ```property
     * Status: string
     * ```
     */
    private fun luaGetStatus(lua: Lua): Int {
        val entry = lua.checkUserdata<LoginQueueEntry>(1)
        lua.push(entry.status, Lua.Conversion.NONE)
        return 1
    }

    /**
     * Last queue message for this user or `nil`.
     *
     * ```property
     * Message: string|nil
     * ```
     */
    private fun luaGetMessage(lua: Lua): Int {
        val entry = lua.checkUserdata<LoginQueueEntry>(1)
        lua.push(entry.message, Lua.Conversion.FULL)
        return 1
    }

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
        getter(::luaGetUserId)
        getter(::luaGetStatus)
        getter(::luaGetMessage)
        callable(::luaNotify)
        callable(::luaAccept)
        callable(::luaReject)
    }

}
