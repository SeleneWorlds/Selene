package com.seleneworlds.server

import org.jline.reader.EndOfFileException
import org.jline.reader.LineReaderBuilder
import org.jline.terminal.TerminalBuilder
import org.koin.mp.KoinPlatform.getKoin
import org.slf4j.Logger
import com.seleneworlds.common.bundles.BundleLifecycleManager
import com.seleneworlds.common.bundles.BundleLoader
import com.seleneworlds.common.lua.LuaManager
import com.seleneworlds.common.network.PacketRegistrations
import com.seleneworlds.common.threading.MainThreadDispatcher
import com.seleneworlds.common.util.Disposable
import com.seleneworlds.server.entities.EntityManager
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread

val startupTime = System.currentTimeMillis()

class SeleneServer(
    private val httpServer: com.seleneworlds.server.http.HttpServer,
    private val networkServer: com.seleneworlds.server.network.NetworkServer,
    private val serverHeartbeat: com.seleneworlds.server.heartbeat.ServerHeartbeat,
    private val bundleWatcher: com.seleneworlds.server.bundle.ServerBundleWatcher,
    private val entityManager: EntityManager,
    packetRegistrations: PacketRegistrations,
    clientBundleCache: com.seleneworlds.server.bundles.ClientBundleCache,
    bundleLoader: BundleLoader,
    private val bundleLifecycleManager: BundleLifecycleManager,
    luaManager: LuaManager,
    private val config: com.seleneworlds.server.config.ServerConfig,
    private val logger: Logger,
    private val mainThreadDispatcher: MainThreadDispatcher
) {

    private val running = AtomicBoolean(true)

    init {
        logger.info("Starting Selene server")
        packetRegistrations.register()
        luaManager.lua.set("SELENE_IS_SERVER", true)
        luaManager.lua.set("SELENE_IS_CLIENT", false)
        luaManager.loadModules()
        val bundles = bundleLoader.resolveBundles(config.bundles.filter { it.isNotBlank() }.toSet())
        bundleLifecycleManager.initializeBundles(bundles)

        clientBundleCache.watchBundles(config.bundles)
    }

    fun start() {
        ServerEvents.ServerStarted.EVENT.invoker().serverStarted()
        ServerEvents.ServerReloaded.EVENT.invoker().serverReloaded()

        httpServer.start()
        networkServer.start(config.port)
        serverHeartbeat.start()
        if (config.hotReloadMode.isEnabled) {
            bundleWatcher.startWatching()
        }

        startConsoleThread()
        startMainEventLoop()
    }

    private fun startConsoleThread() {
        if (System.console() == null) {
            logger.info("No interactive console detected, skipping console thread")
            return
        }

        thread(name = "Console Thread", isDaemon = true) {
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
        var lastTick = System.nanoTime()
        while (running.get()) {
            val now = System.nanoTime()
            entityManager.update((now - lastTick) / 1_000_000_000f)
            lastTick = now
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
