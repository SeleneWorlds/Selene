package com.seleneworlds.common.bundles

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BundleDatabaseTest {

    @Test
    fun returnsTransitiveDependentsWithoutRootBundle() {
        val database = BundleDatabase().apply {
            addBundle(bundle("base"))
            addBundle(bundle("direct", dependencies = listOf("base")))
            addBundle(bundle("transitive", dependencies = listOf("direct")))
            addBundle(bundle("other"))
        }

        assertEquals(
            setOf("direct", "transitive"),
            database.getTransitiveDependents("base")
        )
    }

    @Test
    fun addBundleDefaultsToEnabledForDirectCallers() {
        val database = BundleDatabase()
        database.addBundle(bundle("active"))

        assertEquals(listOf("active"), database.enabledBundles.map { it.manifest.name })
        assertTrue(database.isBundleEnabled("active"))
    }

    @Test
    fun canResolveBundlesWithoutEnablingThem() {
        val database = BundleDatabase()
        val bundle = bundle("inactive")

        database.addBundle(bundle, enabled = false)

        assertEquals(listOf("inactive"), database.resolvedBundles.map { it.manifest.name })
        assertTrue(database.enabledBundles.isEmpty())

        database.enableBundle(bundle)
        assertTrue(database.isBundleEnabled("inactive"))

        database.disableBundle(bundle)
        assertFalse(database.isBundleEnabled("inactive"))
        assertTrue(database.enabledBundles.isEmpty())
    }

    private fun bundle(name: String, dependencies: List<String> = emptyList()): Bundle {
        return Bundle(BundleManifest(name = name, dependencies = dependencies), File(name))
    }
}
