package com.seleneworlds.client.ui

import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.EventListener
import com.seleneworlds.common.bundles.Bundle
import com.seleneworlds.common.bundles.BundleExecutionContext
import com.seleneworlds.common.bundles.BundleStateCleaner

object BundleUiListeners : BundleStateCleaner {
    private data class Registration(
        val bundle: Bundle,
        val removeListener: () -> Unit
    )

    private val lock = Any()
    private val registrations = mutableListOf<Registration>()

    fun record(bundle: Bundle, removeListener: () -> Unit) {
        synchronized(lock) {
            registrations.add(Registration(bundle, removeListener))
        }
    }

    fun addActorListener(actor: Actor, listener: EventListener, bundle: Bundle? = BundleExecutionContext.currentBundle) {
        actor.addListener(listener)
        if (bundle != null) {
            record(bundle) {
                actor.removeListener(listener)
            }
        }
    }

    override fun clearBundleState(bundle: Bundle) {
        val removedRegistrations = synchronized(lock) {
            val removed = registrations.filter { it.bundle == bundle }
            registrations.removeAll(removed.toSet())
            removed
        }
        removedRegistrations.forEach { it.removeListener() }
    }

    fun <T> runInBundleContext(bundle: Bundle?, block: () -> T): T {
        return if (bundle != null) {
            BundleExecutionContext.withBundle(bundle, block)
        } else {
            block()
        }
    }
}
