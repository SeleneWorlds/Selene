package world.selene.common.lua

import world.selene.common.script.ScriptTrace

data class ClosureTrace(val trace: () -> String) : ScriptTrace {
    override fun scriptTrace(): String {
        return trace()
    }
}