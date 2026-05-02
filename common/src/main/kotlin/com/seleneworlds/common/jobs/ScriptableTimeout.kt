package com.seleneworlds.common.jobs

import java.util.concurrent.ScheduledFuture

data class ScriptableTimeout(
    val timeoutId: Int,
    val intervalMs: Int,
    val callback: () -> Unit
) {
    var task: ScheduledFuture<*>? = null
}