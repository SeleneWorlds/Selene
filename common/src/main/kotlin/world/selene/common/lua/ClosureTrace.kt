package world.selene.common.lua

data class ClosureTrace(val trace: () -> String) : LuaTrace {
    override fun luaTrace(): String {
        return trace()
    }
}