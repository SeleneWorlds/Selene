package com.seleneworlds.common.threading

import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.ExecutionException
import java.util.concurrent.FutureTask

class MainThreadDispatcher {
    private val mainThreadQueue = ConcurrentLinkedQueue<Runnable>()
    @Volatile
    private var mainThread: Thread? = null

    fun bindToCurrentThread() {
        mainThread = Thread.currentThread()
    }

    fun runOnMainThread(runnable: Runnable) {
        if (Thread.currentThread() === mainThread) {
            runnable.run()
            return
        }
        mainThreadQueue.offer(runnable)
    }

    fun <T> callOnMainThread(callback: () -> T): T {
        if (Thread.currentThread() === mainThread) {
            return callback()
        }

        val task = FutureTask(callback)
        mainThreadQueue.offer(task)
        try {
            return task.get()
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            throw IllegalStateException("Interrupted while waiting for main thread work", e)
        } catch (e: ExecutionException) {
            throw e.cause ?: e
        }
    }

    fun process() {
        while (true) {
            val runnable = mainThreadQueue.poll() ?: break
            runnable.run()
        }
    }
}
