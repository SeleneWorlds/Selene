package world.selene.common.lua

import party.iroiro.luajava.Lua
import party.iroiro.luajava.value.LuaValue
import world.selene.common.threading.MainThreadDispatcher
import world.selene.common.util.Disposable
import java.time.LocalDateTime
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

class LuaSchedulesModule(
    private val mainThreadDispatcher: MainThreadDispatcher
) : LuaModule, Disposable {
    override val name = "selene.schedules"

    private val executor: ScheduledExecutorService = Executors.newScheduledThreadPool(1)
    private var lastSecond = -1
    private var lastMinute = -1
    private var lastHour = -1
    private val timeouts = mutableSetOf<ScheduledFuture<*>>()

    val secondSignal = Signal("Second")
    val minuteSignal = Signal("Minute")
    val hourSignal = Signal("Hour")

    init {
        executor.scheduleAtFixedRate(::checkTimeIntervals, 0, 1, TimeUnit.SECONDS)
    }

    override fun register(table: LuaValue) {
        table.set("EverySecond", secondSignal)
        table.set("EveryMinute", minuteSignal)
        table.set("EveryHour", hourSignal)
        table.register("SetTimeout", this::luaSetTimeout)
    }

    private fun checkTimeIntervals() {
        val now = LocalDateTime.now()
        val currentSecond = now.second
        val currentMinute = now.minute
        val currentHour = now.hour

        if (lastSecond != currentSecond) {
            lastSecond = currentSecond
            mainThreadDispatcher.runOnMainThread {
                secondSignal.emit()
            }
        }

        if (lastMinute != currentMinute) {
            lastMinute = currentMinute
            mainThreadDispatcher.runOnMainThread {
                minuteSignal.emit()
            }
        }

        if (lastHour != currentHour) {
            lastHour = currentHour
            mainThreadDispatcher.runOnMainThread {
                hourSignal.emit()
            }
        }
    }

    private fun luaSetTimeout(lua: Lua): Int {
        val intervalMs = lua.checkInt(1)
        val callback = lua.checkFunction(2)
        
        if (intervalMs < 0) {
            return lua.error(IllegalArgumentException("Timeout interval must be non-negative"))
        }
        
        val task = executor.schedule({
            mainThreadDispatcher.runOnMainThread {
                callback.call()
            }
        }, intervalMs.toLong(), TimeUnit.MILLISECONDS)
        
        timeouts.add(task)
        timeouts.removeAll { it.isDone }
        return 0
    }

    override fun dispose() {
        timeouts.forEach { it.cancel(false) }
        timeouts.clear()

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
