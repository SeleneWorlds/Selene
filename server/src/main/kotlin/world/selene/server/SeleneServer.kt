package world.selene.server

import arrow.core.prependTo
import org.jline.reader.EndOfFileException
import org.jline.reader.LineReaderBuilder
import org.jline.terminal.TerminalBuilder
import org.koin.mp.KoinPlatform.getKoin
import org.slf4j.Logger
import world.selene.common.bundles.BundleDatabase
import world.selene.common.bundles.BundleLoader
import world.selene.common.data.ComponentRegistry
import world.selene.common.data.CustomRegistries
import world.selene.common.lua.LuaManager
import world.selene.common.network.PacketRegistrations
import world.selene.server.config.ServerConfig
import world.selene.common.data.EntityRegistry
import world.selene.common.data.TileRegistry
import world.selene.common.data.TransitionRegistry
import world.selene.server.bundles.ClientBundleCache
import world.selene.server.data.PersistentNameIdRegistry
import world.selene.server.http.HttpServer
import world.selene.server.lua.ServerLuaSignals
import world.selene.server.network.NetworkServer
import world.selene.common.threading.MainThreadDispatcher
import world.selene.common.util.Disposable
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread

class SeleneServer(
    private val httpServer: HttpServer,
    private val networkServer: NetworkServer,
    private val packetRegistrations: PacketRegistrations,
    private val clientBundleCache: ClientBundleCache,
    private val bundleLoader: BundleLoader,
    private val bundleDatabase: BundleDatabase,
    private val nameIdRegistry: PersistentNameIdRegistry,
    private val tileRegistry: TileRegistry,
    private val transitionRegistry: TransitionRegistry,
    private val componentRegistry: ComponentRegistry,
    private val entityRegistry: EntityRegistry,
    private val customRegistries: CustomRegistries,
    private val luaManager: LuaManager,
    private val signals: ServerLuaSignals,
    private val config: ServerConfig,
    private val logger: Logger,
    private val mainThreadDispatcher: MainThreadDispatcher
)  {

    private val running = AtomicBoolean(true)

    init {
        logger.info("Starting Selene server")
        packetRegistrations.register()
        luaManager.setGlobal("SELENE_IS_SERVER", true)
        luaManager.setGlobal("SELENE_IS_CLIENT", false)
        luaManager.loadModules()
        val bundles = bundleLoader.loadBundles(config.bundles.filter { it.isNotBlank() }.toSet())
        tileRegistry.load(bundleDatabase)
        transitionRegistry.load(bundleDatabase)
        componentRegistry.load(bundleDatabase)
        entityRegistry.load(bundleDatabase)
        customRegistries.load(bundleDatabase)
        customRegistries.loadCustomRegistries(bundleDatabase, "common")
        customRegistries.loadCustomRegistries(bundleDatabase, "server")
        nameIdRegistry.load()
        nameIdRegistry.populate(tileRegistry)
        nameIdRegistry.populate(componentRegistry)
        nameIdRegistry.populate(entityRegistry)
        nameIdRegistry.save()
        bundleLoader.loadBundleEntrypoints(bundles, listOf("common/", "server/", "init.lua"))

        clientBundleCache.watchBundles(config.bundles)
    }

    fun start() {
        signals.serverStarted.emit()
        signals.serverReloaded.emit()

        httpServer.start()
        networkServer.start(config.port)

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
            } catch (_: EndOfFileException) {
                running.set(false)
            }
        }
    }

    private fun startMainEventLoop() {
        while (running.get()) {
            mainThreadDispatcher.process()
            networkServer.process()

            Thread.sleep(10)
        }

        shutdown()
    }

    fun shutdown() {
        running.set(false)
        networkServer.stop()

        getKoin().getAll<Disposable>().forEach { it.dispose() }
    }
}