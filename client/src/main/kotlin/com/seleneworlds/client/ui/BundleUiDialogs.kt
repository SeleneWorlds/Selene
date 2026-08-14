package com.seleneworlds.client.ui

import com.badlogic.gdx.scenes.scene2d.ui.Dialog
import com.seleneworlds.common.bundles.Bundle
import com.seleneworlds.common.bundles.BundleStateCleaner

object BundleUiDialogs : BundleStateCleaner {
    private data class Registration(
        val bundle: Bundle,
        val dialog: Dialog
    )

    private val lock = Any()
    private val registrations = mutableListOf<Registration>()

    fun record(bundle: Bundle, dialog: Dialog) {
        synchronized(lock) {
            registrations.add(Registration(bundle, dialog))
        }
    }

    override fun clearBundleState(bundle: Bundle) {
        val removedRegistrations = synchronized(lock) {
            val removed = registrations.filter { it.bundle == bundle }
            registrations.removeAll(removed.toSet())
            removed
        }
        removedRegistrations.forEach { it.dialog.hide() }
    }
}
