package com.seleneworlds.server

import java.util.concurrent.CompletableFuture

/** Keeps disposal on the server thread after its last tick, and lets shutdown hooks wait for it. */
internal class ServerLifecycle(private val dispose: () -> Unit) {
    private val lock = Any()
    private val finished = CompletableFuture<Unit>()
    private var owner: Thread? = null
    @Volatile
    var running = true
        private set

    fun run(body: () -> Unit) {
        synchronized(lock) {
            if (!running) return
            check(owner == null) { "Server has already started" }
            owner = Thread.currentThread()
        }
        try {
            body()
        } finally {
            running = false
            finish()
        }
    }

    fun shutdown() {
        val disposeHere = synchronized(lock) {
            running = false
            if (owner == null) {
                owner = Thread.currentThread()
                true
            } else {
                // A request from a server callback must return so the loop can finish.
                if (owner === Thread.currentThread()) return
                false
            }
        }
        if (disposeHere) finish()
        finished.join()
    }

    private fun finish() {
        try {
            dispose()
            finished.complete(Unit)
        } catch (failure: Throwable) {
            finished.completeExceptionally(failure)
            throw failure
        }
    }
}
