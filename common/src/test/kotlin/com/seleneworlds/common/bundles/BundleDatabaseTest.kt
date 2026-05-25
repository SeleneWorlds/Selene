package com.seleneworlds.common.bundles

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals

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

    private fun bundle(name: String, dependencies: List<String> = emptyList()): Bundle {
        return Bundle(BundleManifest(name = name, dependencies = dependencies), File(name))
    }
}
