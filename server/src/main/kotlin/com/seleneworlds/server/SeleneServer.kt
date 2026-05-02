package com.seleneworlds.server

import org.jline.reader.EndOfFileException
import org.jline.reader.LineReaderBuilder
import org.jline.terminal.TerminalBuilder
import org.koin.mp.KoinPlatform.getKoin
import org.slf4j.Logger
import com.seleneworlds.common.bundles.BundleDatabase
import com.seleneworlds.common.bundles.BundleLoader
import com.seleneworlds.common.data.custom.CustomRegistries
import com.seleneworlds.common.entities.EntityRegistry
import com.seleneworlds.common.entities.component.ComponentRegistry
import com.seleneworlds.common.lua.LuaManager
import com.seleneworlds.common.network.PacketRegistrations
import com.seleneworlds.common.sounds.SoundRegistry
import com.seleneworlds.common.threading.MainThreadDispatcher
import com.seleneworlds.common.tiles.TileRegistry
import com.seleneworlds.common.tiles.transitions.TransitionRegistry
import com.seleneworlds.common.util.Disposable
import com.seleneworlds.server.bundle.ServerBundleWatcher
import com.seleneworlds.server.bundles.ClientBundleCache
import com.seleneworlds.server.config.ServerConfig
import com.seleneworlds.server.data.mappings.PersistentNameIdRegistry
import com.seleneworlds.server.heartbeat.ServerHeartbeat
import com.seleneworlds.server.http.HttpServer
import com.seleneworlds.server.network.NetworkServer
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread

val startupTime = System.currentTimeMillis()

class SeleneServer(
    private val httpServer: com.seleneworlds.server.http.HttpServer,
    private val networkServer: com.seleneworlds.server.network.NetworkServer,
    private val serverHeartbeat: com.seleneworlds.server.heartbeat.ServerHeartbeat,
    private val bundleWatcher: com.seleneworlds.server.bundle.ServerBundleWatcher,
    packetRegistrations: PacketRegistrations,
    clientBundleCache: com.seleneworlds.server.bundles.ClientBundleCache,
    bundleLoader: BundleLoader,
    bundleDatabase: BundleDatabase,
    nameIdRegistry: com.seleneworlds.server.data.mappings.PersistentNameIdRegistry,
    tileRegistry: TileRegistry,
    transitionRegistry: TransitionRegistry,
    componentRegistry: ComponentRegistry,
    soundRegistry: SoundRegistry,
    entityRegistry: EntityRegistry,
    customRegistries: CustomRegistries,
    luaManager: LuaManager,
    private val config: com.seleneworlds.server.config.ServerConfig,
    logger: Logger,
    private val mainThreadDispatcher: MainThreadDispatcher
) {

    private val running = AtomicBoolean(true)

    init {
        logger.info("Starting Selene server")
        packetRegistrations.register()
        luaManager.lua.set("SELENE_IS_SERVER", true)
        luaManager.lua.set("SELENE_IS_CLIENT", false)
        luaManager.loadModules()
        val bundles = bundleLoader.loadBundles(config.bundles.filter { it.isNotBlank() }.toSet())
        tileRegistry.load(bundleDatabase)
        transitionRegistry.load(bundleDatabase)
        componentRegistry.load(bundleDatabase)
        soundRegistry.load(bundleDatabase)
        entityRegistry.load(bundleDatabase)
        customRegistries.load(bundleDatabase)
        customRegistries.loadCustomRegistries(bundleDatabase, "common")
        customRegistries.loadCustomRegistries(bundleDatabase, "server")
        nameIdRegistry.load()
        nameIdRegistry.populate(tileRegistry)
        nameIdRegistry.populate(componentRegistry)
        nameIdRegistry.populate(entityRegistry)
        nameIdRegistry.populate(soundRegistry)
        nameIdRegistry.save()
        bundleLoader.loadBundleEntrypoints(bundles, listOf("common/", "server/", "init.lua"))

        clientBundleCache.watchBundles(config.bundles)
    }

    fun start() {
        _root_ide_package_.com.seleneworlds.server.ServerEvents.ServerStarted.EVENT.invoker().serverStarted()
        _root_ide_package_.com.seleneworlds.server.ServerEvents.ServerReloaded.EVENT.invoker().serverReloaded()

        httpServer.start()
        networkServer.start(config.port)
        serverHeartbeat.start()
        if (config.hotReload) {
            bundleWatcher.startWatching()
        }

        startConsoleThread()
        startMainEventLoop()
    }

    private fun startConsoleThread() {
        thread(name = "Console Thread") {
            val terminal = TerminalBuilder.builder()
                .system(true)
                .build()

            val reader = LineReaderBuilder.builder()
                .terminal(terminal)
                .build()

            try {
                var line = reader.readLine()
                while (line != null && running.get()) {
                    if (line.equals("exit", ignoreCase = true)) {
                        running.set(false)
                        break
                    }
                    line = reader.readLine()
                }
            } catch (_: EndOfFileException) { }
        }
    }

    private fun startMainEventLoop() {
        mainThreadDispatcher.bindToCurrentThread()
        while (running.get()) {
            mainThreadDispatcher.process()
            networkServer.process()
            bundleWatcher.processPendingUpdates()

            Thread.sleep(10)
        }

        shutdown()
    }

    fun shutdown() {
        running.set(false)
        serverHeartbeat.stop()
        networkServer.stop()

        getKoin().getAll<Disposable>().forEach { it.dispose() }
    }
}
