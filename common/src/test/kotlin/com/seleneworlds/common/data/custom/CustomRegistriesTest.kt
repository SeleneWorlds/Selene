package com.seleneworlds.common.data.custom

import com.seleneworlds.common.bundles.BundleDatabase
import com.seleneworlds.common.bundles.Bundle
import com.seleneworlds.common.bundles.BundleExecutionContext
import com.seleneworlds.common.bundles.BundleManifest
import com.seleneworlds.common.data.Identifier
import com.seleneworlds.common.data.RegistriesLuaApi
import com.seleneworlds.common.data.Registry
import com.seleneworlds.common.data.RegistryReloadListener
import com.seleneworlds.common.lua.LuaEvent
import com.seleneworlds.common.serialization.seleneJson
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import java.io.File

class CustomRegistriesTest {
    @Test
    fun registrySignalListenersCanRunThroughBundleContextProxy() {
        val bundle = Bundle(BundleManifest(name = "test"), File("."))
        var activeBundle: Bundle? = null
        val listener = LuaEvent.withBundleContext(
            RegistriesLuaApi.RegistryReloaded { activeBundle = BundleExecutionContext.currentBundle },
            bundle
        )

        listener.invoke()

        assertSame(bundle, activeBundle)
    }

    @Test
    fun preservesRegistryInstanceAndListenersAcrossRuntimeRebuilds() {
        val registries = CustomRegistries(seleneJson)
        val identifier = Identifier("test", "npcs")
        registries.upsertEntry(identifier, buildJsonObject {
            put("name", "npcs")
            put("platform", "server")
        })
        val bundleDatabase = BundleDatabase()

        registries.loadCustomRegistries(bundleDatabase, "server")
        val registry = registries.getCustomRegistry(identifier) as CustomRegistry
        var reloads = 0
        registry.addReloadListener(object : RegistryReloadListener<CustomRegistryObject> {
            override fun onRegistryReloaded(registry: Registry<CustomRegistryObject>) {
                reloads++
            }
        })

        registries.loadCustomRegistries(bundleDatabase, "server")

        assertSame(registry, registries.getCustomRegistry(identifier))
        assertEquals(1, reloads)
    }

    @Test
    fun loadingOnePlatformDoesNotDiscardOtherPlatforms() {
        val registries = CustomRegistries(seleneJson)
        val commonIdentifier = Identifier("test", "common_data")
        val serverIdentifier = Identifier("test", "server_data")
        registries.upsertEntry(commonIdentifier, definition("common-data", "common"))
        registries.upsertEntry(serverIdentifier, definition("server-data", "server"))
        val bundleDatabase = BundleDatabase()

        registries.loadCustomRegistries(bundleDatabase, "common")
        registries.loadCustomRegistries(bundleDatabase, "server")

        assertSame(registries.getCustomRegistry(commonIdentifier), registries.getAllCustomRegistries().first { it.name == "common-data" })
        assertSame(registries.getCustomRegistry(serverIdentifier), registries.getAllCustomRegistries().first { it.name == "server-data" })
    }

    private fun definition(name: String, platform: String) = buildJsonObject {
        put("name", name)
        put("platform", platform)
    }
}
