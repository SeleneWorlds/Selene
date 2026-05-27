package com.seleneworlds.client.ui

import com.seleneworlds.common.bundles.Bundle
import com.seleneworlds.common.bundles.BundleEventSubscriptions
import com.seleneworlds.common.bundles.BundleExecutionContext
import com.seleneworlds.common.bundles.BundleManifest
import com.seleneworlds.common.event.EventFactory
import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class BundleUiInputProcessorsTest {

    @Test
    fun `clearBundleState removes only matching bundle listeners`() {
        val bundleA = Bundle(BundleManifest(name = "bundle-a"), createTempDirectory())
        val bundleB = Bundle(BundleManifest(name = "bundle-b"), createTempDirectory())
        var bundleARemovals = 0
        var bundleBRemovals = 0

        try {
            BundleUiInputProcessors.record(bundleA) { bundleARemovals++ }
            BundleUiInputProcessors.record(bundleB) { bundleBRemovals++ }

            BundleUiInputProcessors.clearBundleState(bundleA)

            assertEquals(1, bundleARemovals)
            assertEquals(0, bundleBRemovals)

            BundleUiInputProcessors.clearBundleState(bundleA)
            BundleUiInputProcessors.clearBundleState(bundleB)

            assertEquals(1, bundleARemovals)
            assertEquals(1, bundleBRemovals)
        } finally {
            bundleA.dir.deleteRecursively()
            bundleB.dir.deleteRecursively()
        }
    }

    @Test
    fun `clearBundleState for bundles removes each matching listener`() {
        val bundleA = Bundle(BundleManifest(name = "bundle-a"), createTempDirectory())
        val bundleB = Bundle(BundleManifest(name = "bundle-b"), createTempDirectory())
        val bundleC = Bundle(BundleManifest(name = "bundle-c"), createTempDirectory())
        var bundleARemovals = 0
        var bundleBRemovals = 0
        var bundleCRemovals = 0

        try {
            BundleUiInputProcessors.record(bundleA) { bundleARemovals++ }
            BundleUiInputProcessors.record(bundleB) { bundleBRemovals++ }
            BundleUiInputProcessors.record(bundleC) { bundleCRemovals++ }

            BundleUiInputProcessors.clearBundleState(listOf(bundleA, bundleB))

            assertEquals(1, bundleARemovals)
            assertEquals(1, bundleBRemovals)
            assertEquals(0, bundleCRemovals)
        } finally {
            BundleUiInputProcessors.clearBundleState(bundleA)
            BundleUiInputProcessors.clearBundleState(bundleB)
            BundleUiInputProcessors.clearBundleState(bundleC)
            bundleA.dir.deleteRecursively()
            bundleB.dir.deleteRecursively()
            bundleC.dir.deleteRecursively()
        }
    }

    @Test
    fun `clearBundleState after re-registering does not stack removals`() {
        val bundle = Bundle(BundleManifest(name = "bundle-a"), createTempDirectory())
        var removals = 0

        try {
            BundleUiInputProcessors.record(bundle) { removals++ }
            BundleUiInputProcessors.clearBundleState(bundle)

            BundleUiInputProcessors.record(bundle) { removals++ }
            BundleUiInputProcessors.clearBundleState(bundle)
            BundleUiInputProcessors.clearBundleState(bundle)

            assertEquals(2, removals)
        } finally {
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

            BundleUiInputProcessors.runInBundleContext(bundle) {
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
