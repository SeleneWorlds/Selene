package com.seleneworlds.common.bundles

import com.seleneworlds.common.event.Event

object BundleEventSubscriptions {
    class Subscription internal constructor(
        val bundle: Bundle,
        val event: Event<*>,
        val listener: Any
    ) {
        fun unregister() {
            @Suppress("UNCHECKED_CAST")
            (event as Event<Any>).unregister(listener)
        }
    }

    private val lock = Any()
    private val subscriptions = mutableListOf<Subscription>()

    fun <T : Any> record(event: Event<T>, listener: T) {
        val bundle = BundleExecutionContext.currentBundle ?: return
        synchronized(lock) {
            subscriptions.add(Subscription(bundle, event, listener))
        }
    }

    fun getSubscriptions(bundle: Bundle): List<Subscription> {
        synchronized(lock) {
            return subscriptions.filter { it.bundle == bundle }
        }
    }

    fun removeSubscriptions(bundle: Bundle): List<Subscription> {
        synchronized(lock) {
            val removedSubscriptions = subscriptions.filter { it.bundle == bundle }
            subscriptions.removeAll(removedSubscriptions.toSet())
            return removedSubscriptions
        }
    }

    fun unregisterSubscriptions(bundle: Bundle) {
        removeSubscriptions(bundle).forEach { it.unregister() }
    }
}
