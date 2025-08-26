package world.selene.common.lua

import party.iroiro.luajava.Lua

data class CallerInfo(val source: String, val line: Int) {
    override fun toString(): String {
        return "$source:$line"
    }
}

fun Lua.getCallerInfo(offset: Int = 2): CallerInfo {
    LuaManager.debugLibrary.push(this)
    getField(-1, "getinfo")
    push(offset)
    push("Sl")
    pCall(2, 1)

    if (isNil(-1)) {
        pop(2)
        return CallerInfo("?", 0)
    }

    getField(-1, "short_src")
    val source = toString(-1)!!
    pop(1) // short_src

    getField(-1, "currentline")
    val line = toNumber(-1).toInt()
    pop(1) // currentline

    pop(1) // debug
    return CallerInfo(source, line)
}