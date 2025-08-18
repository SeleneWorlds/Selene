package world.selene.common.threading

import java.util.concurrent.ConcurrentLinkedQueue

class MainThreadDispatcher {
    private val mainThreadQueue = ConcurrentLinkedQueue<Runnable>()

    fun runOnMainThread(runnable: Runnable) {
        mainThreadQueue.offer(runnable)
    }

    fun process() {
        while (true) {
            val runnable = mainThreadQueue.poll() ?: break
            runnable.run()
        }
    }
}
