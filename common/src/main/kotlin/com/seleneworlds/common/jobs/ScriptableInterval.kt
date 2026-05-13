package com.seleneworlds.common.jobs

import com.seleneworlds.common.bundles.Bundle
import java.util.concurrent.ScheduledFuture

data class ScriptableInterval(
    val intervalId: Int,
    val intervalMs: Int,
    val bundle: Bundle?,
    val callback: () -> Unit,
) {
    var task: ScheduledFuture<*>? = null
}
