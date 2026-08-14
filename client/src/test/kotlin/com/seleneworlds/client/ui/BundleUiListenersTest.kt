package com.seleneworlds.client.ui

import com.badlogic.gdx.scenes.scene2d.Actor
import com.seleneworlds.common.bundles.Bundle
import com.seleneworlds.common.bundles.BundleEventSubscriptions
import com.seleneworlds.common.bundles.BundleExecutionContext
import com.seleneworlds.common.bundles.BundleManifest
import com.seleneworlds.common.event.EventFactory
import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BundleUiListenersTest {

    @Test
    fun `clearBundleState removes only matching bundle listeners`() {
        val bundleA = Bundle(BundleManifest(name = "bundle-a"), createTempDirectory())
        val bundleB = Bundle(BundleManifest(name = "bundle-b"), createTempDirectory())
        var bundleARemovals = 0
        var bundleBRemovals = 0

        try {
            BundleUiListeners.record(bundleA) { bundleARemovals++ }
            BundleUiListeners.record(bundleB) { bundleBRemovals++ }

            BundleUiListeners.clearBundleState(bundleA)

            assertEquals(1, bundleARemovals)
            assertEquals(0, bundleBRemovals)

            BundleUiListeners.clearBundleState(bundleA)
            BundleUiListeners.clearBundleState(bundleB)

            assertEquals(1, bundleARemovals)
            assertEquals(1, bundleBRemovals)
        } finally {
            bundleA.dir.deleteRecursively()
            bundleB.dir.deleteRecursively()
        }
    }

    @Test
    fun `clearBundleState after re-registering does not stack removals`() {
        val bundle = Bundle(BundleManifest(name = "bundle-a"), createTempDirectory())
        var removals = 0

        try {
            BundleUiListeners.record(bundle) { removals++ }
            BundleUiListeners.clearBundleState(bundle)

            BundleUiListeners.record(bundle) { removals++ }
            BundleUiListeners.clearBundleState(bundle)
            BundleUiListeners.clearBundleState(bundle)

            assertEquals(2, removals)
        } finally {
            bundle.dir.deleteRecursively()
        }
    }

    @Test
    fun `addActorListener removes bundle-owned actor listeners`() {
        val bundle = Bundle(BundleManifest(name = "bundle-a"), createTempDirectory())
        val actor = Actor()

        try {
            BundleExecutionContext.withBundle(bundle) {
                BundleUiListeners.addActorListener(actor, { false })
            }

            assertEquals(1, actor.listeners.size)

            BundleUiListeners.clearBundleState(bundle)

            assertEquals(0, actor.listeners.size)
        } finally {
            bundle.dir.deleteRecursively()
        }
    }

    @Test
    fun `runInBundleContext restores previous bundle after actor listener callback`() {
        val bundle = Bundle(BundleManifest(name = "bundle-a"), createTempDirectory())
        val actor = Actor()
        var callbackRanInBundle = false

        try {
            BundleUiListeners.addActorListener(actor, { _ ->
                BundleUiListeners.runInBundleContext(bundle) {
                    callbackRanInBundle = BundleExecutionContext.currentBundle == bundle
                }
                false
            }, bundle)

            assertNull(BundleExecutionContext.currentBundle)
            assertFalse(actor.fire(com.badlogic.gdx.scenes.scene2d.Event()))
            assertTrue(callbackRanInBundle)
            assertNull(BundleExecutionContext.currentBundle)
        } finally {
            BundleUiListeners.clearBundleState(bundle)
            bundle.dir.deleteRecursively()
        }
    }

    @Test
    fun `runInBundleContext attributes nested registrations to bundle`() {
        val bundle = Bundle(BundleManifest(name = "bundle-a"), createTempDirectory())
        val event = EventFactory.arrayBackedEvent<Listener> { listeners ->
            Listener { listeners.forEach { it.invoke() } }
        }

        try {
            val listener = Listener { }
            assertNull(BundleExecutionContext.currentBundle)

            BundleUiListeners.runInBundleContext(bundle) {
                assertEquals(bundle, BundleExecutionContext.currentBundle)
                event.register(listener)
                BundleEventSubscriptions.record(event, listener)
            }

            assertEquals(1, BundleEventSubscriptions.getSubscriptions(bundle).size)
        } finally {
            BundleEventSubscriptions.clearBundleState(bundle)
            bundle.dir.deleteRecursively()
        }
    }

    private fun interface Listener {
        fun invoke()
    }

    private fun createTempDirectory(): File = Files.createTempDirectory("bundle-ui-input-processors-test").toFile()
}
