package world.selene.client

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.slf4j.Logger
import world.selene.client.config.ClientRuntimeConfig
import world.selene.client.bundle.ClientBundleWatcher
import world.selene.client.config.ClientConfig
import world.selene.client.sounds.AudioRegistry
import world.selene.client.rendering.visual.VisualRegistry
import world.selene.client.network.NetworkClient
import world.selene.client.network.NetworkClientImpl
import world.selene.client.rendering.drawable.DrawableManager
import world.selene.client.rendering.visual.VisualDefinition
import world.selene.common.bundles.BundleDatabase
import world.selene.common.bundles.BundleLoader
import world.selene.common.data.Identifier
import world.selene.common.data.Registry
import world.selene.common.data.RegistryReloadListener
import world.selene.common.data.custom.CustomRegistries
import world.selene.common.entities.EntityRegistry
import world.selene.common.entities.component.ComponentRegistry
import world.selene.common.lua.LuaManager
import world.selene.common.network.PacketHandler
import world.selene.common.network.PacketRegistrations
import world.selene.common.network.packet.AuthenticatePacket
import world.selene.common.network.packet.FinalizeJoinPacket
import world.selene.common.network.packet.PreferencesPacket
import world.selene.common.sounds.SoundRegistry
import world.selene.common.tiles.TileRegistry
import java.util.*
import kotlin.math.min
import kotlin.math.pow

class SeleneClient(
    private val config: ClientConfig,
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
    private val bundleWatcher: ClientBundleWatcher,
    private val drawableManager: DrawableManager,
    private val logger: Logger
) {
    fun start() {
        logger.info("Starting Selene Client")

        packetRegistrations.register()
        luaManager.lua.set("SELENE_IS_CLIENT", true)
        luaManager.lua.set("SELENE_IS_SERVER", false)
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
        if (config.hotReload) {
            bundleWatcher.startWatching()
        }

        visualRegistry.addReloadListener(object: RegistryReloadListener<VisualDefinition> {
            override fun onRegistryReloaded(registry: Registry<VisualDefinition>) {
                drawableManager.clearSharedIdentifiers()
            }

            override fun onEntryAdded(
                registry: Registry<VisualDefinition>,
                identifier: Identifier,
                newData: VisualDefinition
            ) {
                drawableManager.clearSharedIdentifier(identifier)
            }

            override fun onEntryChanged(
                registry: Registry<VisualDefinition>,
                identifier: Identifier,
                oldData: VisualDefinition,
                newData: VisualDefinition
            ) {
                drawableManager.clearSharedIdentifier(identifier)
            }

            override fun onEntryRemoved(
                registry: Registry<VisualDefinition>,
                identifier: Identifier,
                oldData: VisualDefinition
            ) {
                drawableManager.clearSharedIdentifier(identifier)
            }
        })

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