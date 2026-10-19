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
import com.seleneworlds.server.bundle.ServerBundleWatcher
import com.seleneworlds.server.bundles.ClientBundleCache
import com.seleneworlds.server.config.ServerConfig
import com.seleneworlds.server.discord.DiscordBot
import com.seleneworlds.server.entities.EntityManager
import com.seleneworlds.server.heartbeat.ServerHeartbeat
import com.seleneworlds.server.http.HttpServer
import com.seleneworlds.server.network.NetworkServer
import com.seleneworlds.server.profiling.ServerProfiler
import kotlin.concurrent.thread

val startupTime = System.currentTimeMillis()

class SeleneServer(
    private val httpServer: HttpServer,
    private val networkServer: NetworkServer,
    private val serverHeartbeat: ServerHeartbeat,
    private val discordBot: DiscordBot,
    private val bundleWatcher: ServerBundleWatcher,
    private val entityManager: EntityManager,
    packetRegistrations: PacketRegistrations,
    clientBundleCache: ClientBundleCache,
    bundleLoader: BundleLoader,
    private val bundleLifecycleManager: BundleLifecycleManager,
    luaManager: LuaManager,
    private val config: ServerConfig,
    private val logger: Logger,
    private val profiler: ServerProfiler,
    private val mainThreadDispatcher: MainThreadDispatcher
) {

    private val lifecycle = ServerLifecycle(::dispose)

    init {
        logger.info("Starting Selene server")
        packetRegistrations.register()
        luaManager.lua.set("SELENE_IS_SERVER", true)
        luaManager.lua.set("SELENE_IS_CLIENT", false)
        luaManager.loadModules()
        val bundles = bundleLoader.resolveBundles(config.bundles.filter { it.isNotBlank() }.toSet())
        bundleLifecycleManager.initializeBundles(bundles)

        clientBundleCache.watchBundles(bundles)
    }

    fun start() = lifecycle.run {
        ServerEvents.ServerStarted.EVENT.invoker().serverStarted()
        ServerEvents.ServerReloaded.EVENT.invoker().serverReloaded()

        httpServer.start()
        networkServer.start(config.port)
        serverHeartbeat.start()
        discordBot.start()
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
                while (line != null && lifecycle.running) {
                    if (line.equals("exit", ignoreCase = true)) {
                        lifecycle.shutdown()
                        break
                    }
                    line = reader.readLine()
                }
            } catch (_: EndOfFileException) { }
        }
    }

    private fun startMainEventLoop() {
        mainThreadDispatcher.bindToCurrentThread()
        if (config.profilingCaptureSeconds > 0) profiler.startCapture(config.profilingCaptureSeconds)
        var lastTick = System.nanoTime()
        while (lifecycle.running) {
            val now = System.nanoTime()
            profiler.measure(ENTITY_UPDATE) { entityManager.update((now - lastTick) / 1_000_000_000f) }
            lastTick = now
            profiler.measure(DISPATCH) { mainThreadDispatcher.process() }
            profiler.measure(NETWORK) { networkServer.process() }
            profiler.measure(RELOAD) { bundleWatcher.processPendingUpdates() }

            Thread.sleep(10)
        }
    }

    fun shutdown() = lifecycle.shutdown()

    private fun dispose() {
        serverHeartbeat.stop()
        discordBot.dispose()
        networkServer.stop()

        getKoin().getAll<Disposable>().forEach { it.dispose() }
    }
    companion object {
        private val ENTITY_UPDATE = ServerProfiler.Scope("main loop", "entity updates")
        private val DISPATCH = ServerProfiler.Scope("main loop", "queued callbacks")
        private val NETWORK = ServerProfiler.Scope("main loop", "network processing")
        private val RELOAD = ServerProfiler.Scope("main loop", "bundle reloads")
    }

}
