package world.selene.common.jobs

import world.selene.common.threading.MainThreadDispatcher
import world.selene.common.util.Disposable
import java.time.LocalDateTime
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

/**
 * Schedule functions for timeouts, intervals, and periodic events.
 */
class SchedulesApi(
    private val mainThreadDispatcher: MainThreadDispatcher
) : Disposable {

    private val executor: ScheduledExecutorService = Executors.newScheduledThreadPool(1)
    private var lastSecond = -1
    private var lastMinute = -1
    private var lastHour = -1
    private val timeouts = mutableMapOf<Int, ScriptableTimeout>()
    private val intervals = mutableMapOf<Int, ScriptableInterval>()
    private var nextTimeoutId = 1
    private var nextIntervalId = 1

    init {
        executor.scheduleAtFixedRate(::runPeriodicSignals, 0, 1, TimeUnit.SECONDS)
    }

    private fun runPeriodicSignals() {
        val now = LocalDateTime.now()
        val currentSecond = now.second
        val currentMinute = now.minute
        val currentHour = now.hour

        if (lastSecond != currentSecond) {
            lastSecond = currentSecond
            mainThreadDispatcher.runOnMainThread {
                ScheduleEvents.Second.EVENT.invoker().second()
            }
        }

        if (lastMinute != currentMinute) {
            lastMinute = currentMinute
            mainThreadDispatcher.runOnMainThread {
                ScheduleEvents.Minute.EVENT.invoker().minute()
            }
        }

        if (lastHour != currentHour) {
            lastHour = currentHour
            mainThreadDispatcher.runOnMainThread {
                ScheduleEvents.Hour.EVENT.invoker().hour()
            }
        }
    }

    fun setTimeout(
        intervalMs: Int,
        callback: () -> Unit
    ): Int {
        if (intervalMs < 0) {
            throw IllegalArgumentException("Timeout interval must be non-negative")
        }
        val timeoutId = nextTimeoutId++
        val handler = ScriptableTimeout(timeoutId,  intervalMs, callback)

        val task = executor.schedule({
            mainThreadDispatcher.runOnMainThread(callback)
            timeouts.remove(timeoutId)
        }, intervalMs.toLong(), TimeUnit.MILLISECONDS)

        handler.task = task
        timeouts[timeoutId] = handler
        return timeoutId
    }

    fun clearTimeout(timeoutId: Int) {
        val handler = timeouts.remove(timeoutId)
        handler?.task?.cancel(false)
    }

    fun setInterval(
        intervalMs: Int,
        immediate: Boolean = false,
        callback: () -> Unit,
    ): Int {
        if (intervalMs <= 0) {
            throw IllegalArgumentException("Interval must be positive")
        }
        val intervalId = nextIntervalId++
        val handler = ScriptableInterval(intervalId, intervalMs, callback)

        val task = executor.scheduleAtFixedRate({
            mainThreadDispatcher.runOnMainThread(callback)
        }, intervalMs.toLong(), intervalMs.toLong(), TimeUnit.MILLISECONDS)

        handler.task = task
        intervals[intervalId] = handler

        if (immediate) {
            callback()
        }

        return intervalId
    }

    fun clearInterval(intervalId: Int) {
        val handler = intervals.remove(intervalId)
        handler?.task?.cancel(false)
    }

    override fun dispose() {
        timeouts.values.forEach { it.task?.cancel(false) }
        timeouts.clear()

        intervals.values.forEach { it.task?.cancel(false) }
        intervals.clear()

        executor.shutdown()
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow()
            }
        } catch (_: InterruptedException) {
            executor.shutdownNow()
            Thread.currentThread().interrupt()
        }
    }
}
