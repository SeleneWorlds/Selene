package com.seleneworlds.common.bundles

import com.seleneworlds.common.event.EventFactory
import com.seleneworlds.common.lua.LuaManager
import com.seleneworlds.common.lua.libraries.LuaPackageModule
import org.slf4j.LoggerFactory
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals

class BundleLoaderTest {

    @Test
    fun `clearBundleState unregisters bundle event subscriptions`() {
        val tempDir = Files.createTempDirectory("bundle-loader-test")
        val bundle = Bundle(BundleManifest(name = "test-bundle"), tempDir.toFile())
        val luaPackage = LuaPackageModule()
        val luaManager = LuaManager(luaPackage)
        val event = EventFactory.arrayBackedEvent<Listener> { listeners ->
            Listener { listeners.forEach { it.invoke() } }
        }

        try {
            val loader = BundleLoader(
                logger = LoggerFactory.getLogger(BundleLoaderTest::class.java),
                luaManager = luaManager,
                luaPackage = luaPackage,
                bundleDatabase = BundleDatabase(),
                bundleLocator = object : BundleLocator {
                    override fun locateBundle(name: String): Bundle? = null
                }
            )

            var calls = 0
            val listener = BundleExecutionContext.withBundle(bundle) {
                val scopedListener = Listener { calls++ }
                event.register(scopedListener)
                BundleEventSubscriptions.record(event, scopedListener)
                scopedListener
            }

            event.invoker().invoke()
            assertEquals(1, calls)

            loader.clearBundleState(bundle)

            event.invoker().invoke()
            assertEquals(1, calls)

            BundleEventSubscriptions.removeSubscriptions(bundle).forEach { it.unregister() }
            event.unregister(listener)
        } finally {
            luaManager.lua.close()
            tempDir.toFile().deleteRecursively()
        }
    }

    private fun interface Listener {
        fun invoke()
    }
}
