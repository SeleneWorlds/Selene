package world.selene.common.lua

interface LuaTrace {
    fun luaTrace(): String
}

data class ClosureTrace(val trace: () -> String) : LuaTrace {
    override fun luaTrace(): String {
        return trace()
    }
}