package com.seleneworlds.common.network

import com.seleneworlds.common.bundles.Bundle
import com.seleneworlds.common.bundles.BundleExecutionContext
import com.seleneworlds.common.bundles.BundleManifest
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PayloadHandlerRegistryTest {
    @Test
    fun `clears handlers registered by a reloaded bundle`() {
        val registry = PayloadHandlerRegistry<Unit>()
        val bundleA = Bundle(BundleManifest(name = "bundle-a"), Files.createTempDirectory("payload-bundle-a").toFile())
        val bundleB = Bundle(BundleManifest(name = "bundle-b"), Files.createTempDirectory("payload-bundle-b").toFile())
        val received = mutableListOf<String>()

        try {
            BundleExecutionContext.withBundle(bundleA) {
                registry.registerHandler("chat") { _, _ -> received += "a" }
            }
            BundleExecutionContext.withBundle(bundleB) {
                registry.registerHandler("chat") { _, _ -> received += "b" }
            }
            registry.registerHandler("chat") { _, _ -> received += "unowned" }

            registry.clearBundleState(bundleA)
            assertTrue(registry.dispatch("chat", Unit, emptyMap()))

            assertEquals(listOf("b", "unowned"), received)
        } finally {
            bundleA.dir.deleteRecursively()
            bundleB.dir.deleteRecursively()
        }
    }

    @Test
    fun `supports multiple removable handlers for one payload`() {
        val registry = PayloadHandlerRegistry<Unit>()
        val received = mutableListOf<String>()
        val removeFirst = registry.registerHandler("chat") { _, _ -> received += "first" }
        registry.registerHandler("chat") { _, _ -> received += "second" }

        assertTrue(registry.dispatch("chat", Unit, emptyMap()))
        removeFirst()
        assertTrue(registry.dispatch("chat", Unit, emptyMap()))

        assertEquals(listOf("first", "second", "second"), received)
    }

    @Test
    fun `subscription changes during dispatch apply to the next payload`() {
        val registry = PayloadHandlerRegistry<Unit>()
        val received = mutableListOf<String>()
        lateinit var removeSecond: () -> Unit
        registry.registerHandler("chat") { _, _ ->
            received += "first"
            removeSecond()
        }
        removeSecond = registry.registerHandler("chat") { _, _ -> received += "second" }

        registry.dispatch("chat", Unit, emptyMap())
        registry.dispatch("chat", Unit, emptyMap())

        assertEquals(listOf("first", "second", "first"), received)
    }
}
