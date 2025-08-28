package world.selene.client

import kotlinx.coroutines.runBlocking
import org.slf4j.Logger
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
            networkClient.connect(runtimeConfig.host, runtimeConfig.port)
            networkClient.send(AuthenticatePacket(runtimeConfig.token))
            networkClient.send(PreferencesPacket(Locale.getDefault().toString()))
        }
    }
}