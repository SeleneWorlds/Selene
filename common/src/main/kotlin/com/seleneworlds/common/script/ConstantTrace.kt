package com.seleneworlds.common.script

data class ConstantTrace(val trace: String) : ScriptTrace {
    override fun scriptTrace(): String {
        return trace
    }
}
