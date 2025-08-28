package world.selene.client

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.delay
import org.slf4j.Logger
import kotlin.math.min
import kotlin.math.pow
import world.selene.client.config.ClientRuntimeConfig
import world.selene.client.data.AudioRegistry
import world.selene.client.data.VisualRegistry
import world.selene.common.data.TileRegistry
import world.selene.client.network.NetworkClient
import world.selene.client.network.NetworkClientImpl
import world.selene.common.bundles.BundleDatabase
import world.selene.common.bundles.BundleLoader
import world.selene.common.data.ComponentRegistry
import world.selene.common.data.CustomRegistries
import world.selene.common.data.EntityRegistry
import world.selene.common.data.SoundRegistry
import world.selene.common.lua.LuaManager
import world.selene.common.network.PacketHandler
import world.selene.common.network.PacketRegistrations
import world.selene.common.network.packet.AuthenticatePacket
import world.selene.common.network.packet.FinalizeJoinPacket
import world.selene.common.network.packet.PreferencesPacket
import java.util.Locale

class SeleneClient(
    private val networkClient: NetworkClient,
    private val bundleLoader: BundleLoader,
    private val bundleDatabase: BundleDatabase,
    private val luaManager: LuaManager,
    private val packetRegistrations: PacketRegistrations,
    private val tileRegistry: TileRegistry,
    private val componentRegistry: ComponentRegistry,
    private val soundRegistry: SoundRegistry,
    private val entityRegistry: EntityRegistry,
    private val visualRegistry: VisualRegistry,
    private val audioRegistry: AudioRegistry,
    private val customRegistries: CustomRegistries,
    private val runtimeConfig: ClientRuntimeConfig,
    private val packetHandler: PacketHandler<NetworkClient>,
    private val logger: Logger
) {
    fun start() {
        logger.info("Starting Selene Client")

        packetRegistrations.register()
        luaManager.setGlobal("SELENE_IS_CLIENT", true)
        luaManager.setGlobal("SELENE_IS_SERVER", false)
        luaManager.loadModules()
        val bundles = bundleLoader.loadBundles(runtimeConfig.bundles.keys)
        tileRegistry.load(bundleDatabase)
        componentRegistry.load(bundleDatabase)
        soundRegistry.load(bundleDatabase)
        entityRegistry.load(bundleDatabase)
        visualRegistry.load(bundleDatabase)
        audioRegistry.load(bundleDatabase)
        customRegistries.load(bundleDatabase)
        customRegistries.loadCustomRegistries(bundleDatabase, "common")
        customRegistries.loadCustomRegistries(bundleDatabase, "client")
        bundleLoader.loadBundleEntrypoints(bundles, listOf("common/", "client/", "init.lua"))
        (networkClient as NetworkClientImpl).packetHandler = packetHandler

        runBlocking {
            connectWithRetry()
        }
    }

    private suspend fun connectWithRetry() {
        var attempt = 0
        val maxAttempts = 10
        val baseDelayMs = 1000L
        val maxDelayMs = 30000L

        while (attempt < maxAttempts) {
            attempt++
            if (attempt == 1) {
                logger.info("Connecting to ${runtimeConfig.host}:${runtimeConfig.port}")
            } else {
                logger.info("Connecting to ${runtimeConfig.host}:${runtimeConfig.port} (attempt $attempt/$maxAttempts)")
            }

            try {
                val future = networkClient.connect(runtimeConfig.host, runtimeConfig.port).addListener {
                    if (it.isSuccess) {
                        if (attempt == 1) {
                            logger.info("Successfully connected")
                        } else {
                            logger.info("Successfully connected after $attempt attempts")
                        }
                        networkClient.send(AuthenticatePacket(runtimeConfig.token))
                        networkClient.send(PreferencesPacket(Locale.getDefault().toString()))
                        networkClient.send(FinalizeJoinPacket())
                    }
                }.await()
                if (future.isSuccess) {
                    return
                }
            } catch (e: Exception) {
                logger.warn("Connection failed: ${e.message}")
            }

            if (attempt < maxAttempts) {
                val exponentialDelay = (baseDelayMs * 2.0.pow(attempt - 1)).toLong()
                val finalDelay = min(exponentialDelay, maxDelayMs)

                logger.info("Retrying connection in ${finalDelay}ms...")
                delay(finalDelay)
            }
        }

        logger.error("Failed to connect after $maxAttempts attempts. Giving up.")
        throw RuntimeException("Unable to connect to server after $maxAttempts attempts")
    }
}