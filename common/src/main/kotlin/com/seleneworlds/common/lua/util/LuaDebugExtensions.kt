package com.seleneworlds.common.lua.util

import party.iroiro.luajava.Lua
import com.seleneworlds.common.script.ScriptTrace

data class CallerInfo(val source: String, val line: Int) : ScriptTrace {
    override fun toString(): String {
        return "$source:$line"
    }

    override fun scriptTrace(): String {
        return toString()
    }
}

fun Lua.getCallerInfo(offset: Int = 2): CallerInfo {
    getGlobal("debug")
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