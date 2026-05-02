package world.selene.common.script

data class ClosureTrace(val trace: () -> String) : ScriptTrace {
    override fun scriptTrace(): String {
        return trace()
    }
}