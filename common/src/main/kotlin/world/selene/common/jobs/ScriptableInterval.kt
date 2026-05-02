package world.selene.common.jobs

import java.util.concurrent.ScheduledFuture

data class ScriptableInterval(
    val intervalId: Int,
    val intervalMs: Int,
    val callback: () -> Unit,
) {
    var task: ScheduledFuture<*>? = null
}