package world.selene.client

import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.ApplicationListener
import com.badlogic.gdx.InputMultiplexer
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.koin.dsl.module
import org.slf4j.LoggerFactory
import world.selene.client.config.ClientConfig

import com.sksamuel.hoplite.ExperimentalHoplite
import ktx.assets.async.AssetStorage
import ktx.async.MainDispatcher
import org.koin.core.context.startKoin
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf

import org.koin.logger.slf4jLogger
import world.selene.client.assets.AssetProvider
import world.selene.client.assets.BundleFileResolver
import world.selene.client.bundles.ClientBundleLocator
import world.selene.client.camera.CameraManager
import world.selene.client.config.ClientRuntimeConfig
import world.selene.client.controls.GridMovement
import world.selene.client.controls.PlayerController
import world.selene.client.data.Registries
import world.selene.common.data.ComponentRegistry
import world.selene.common.data.TileRegistry
import world.selene.client.data.VisualRegistry
import world.selene.client.grid.Grid
import world.selene.client.input.InputManager
import world.selene.client.lua.ClientLuaSignals
import world.selene.client.lua.LuaCameraModule
import world.selene.client.lua.LuaMapModule
import world.selene.client.lua.LuaTexturesModule
import world.selene.client.lua.LuaUIModule
import world.selene.client.network.ClientPacketHandler
import world.selene.client.network.NetworkClient
import world.selene.client.network.NetworkClientImpl
import world.selene.client.ui.UI
import world.selene.common.bundles.BundleDatabase
import world.selene.common.bundles.BundleLoader
import world.selene.common.bundles.BundleLocator
import world.selene.common.lua.LuaManager
import world.selene.common.lua.LuaMixinModule
import world.selene.common.lua.LuaMixinRegistry
import world.selene.common.lua.LuaModule
import world.selene.client.lua.LuaClientNetworkModule
import world.selene.client.lua.LuaEntitiesModule
import world.selene.client.lua.LuaGameModule
import world.selene.client.lua.LuaGridModule
import world.selene.client.lua.LuaInputModule
import world.selene.client.lua.LuaMovementGridModule
import world.selene.client.maps.ClientMap
import world.selene.client.maps.Entity
import world.selene.client.maps.EntityPool
import world.selene.client.maps.TilePool
import world.selene.client.rendering.DebugRenderer
import world.selene.client.rendering.SceneRenderer
import world.selene.client.scene.Scene
import world.selene.client.visual.VisualManager
import world.selene.common.data.EntityRegistry
import world.selene.common.data.NameIdRegistry
import world.selene.common.lua.LuaPayloadRegistry
import world.selene.common.lua.LuaResourcesModule
import world.selene.common.network.PacketFactory
import world.selene.common.network.PacketHandler
import world.selene.common.network.PacketRegistrations

class SeleneApplication(
    private val config: ClientConfig,
    private val runtimeConfig: ClientRuntimeConfig
) : ApplicationListener {

    var listener: ApplicationListener = object : ApplicationAdapter() {}

    @OptIn(ExperimentalHoplite::class)
    override fun create() {
        MainDispatcher.initiate()

        val coreModule = module {
            single { LoggerFactory.getLogger("Selene") }
        }
        val luaModule = module {
            singleOf(::LuaManager)
            singleOf(::LuaMixinRegistry)
            singleOf(::LuaPayloadRegistry)
            singleOf(::ClientLuaSignals)
            singleOf(::LuaUIModule) { bind<LuaModule>() }
            singleOf(::LuaMixinModule) { bind<LuaModule>() }
            singleOf(::LuaClientNetworkModule) { bind<LuaModule>() }
            singleOf(::LuaCameraModule) { bind<LuaModule>() }
            singleOf(::LuaMapModule) { bind<LuaModule>() }
            singleOf(::LuaTexturesModule) { bind<LuaModule>() }
            singleOf(::LuaResourcesModule) { bind<LuaModule>() }
            singleOf(::LuaInputModule) { bind<LuaModule>() }
            singleOf(::LuaGridModule) { bind<LuaModule>() }
            singleOf(::LuaMovementGridModule) { bind<LuaModule>() }
            singleOf(::LuaGameModule) { bind<LuaModule>() }
            singleOf(::LuaEntitiesModule) { bind<LuaModule>() }
        }
        val bundleModule = module {
            singleOf(::BundleLoader)
            singleOf(::BundleDatabase)
            singleOf(::ClientBundleLocator) { bind<BundleLocator>() }
        }
        val networkModule = module {
            singleOf(::NetworkClientImpl) { bind<NetworkClient>() }
            singleOf(::PacketFactory)
            singleOf(::ClientPacketHandler) { bind<PacketHandler<*>>() }
            singleOf(::PacketRegistrations)
        }
        val dataModule = module {
            single { ObjectMapper().registerKotlinModule() }
            singleOf(::TileRegistry)
            singleOf(::EntityRegistry)
            singleOf(::ComponentRegistry)
            singleOf(::VisualRegistry)
            singleOf(::NameIdRegistry)
            singleOf(::Registries)
        }
        val clientModule = module {
            single { config }
            single { runtimeConfig }
            singleOf(::UI)
            singleOf(::SeleneClient)
        }
        val gdxModule = module {
            singleOf(::SeleneApplicationListener) { bind<ApplicationListener>() }
            singleOf(::BundleFileResolver)
            single { AssetStorage(fileResolver = get<BundleFileResolver>()) }
            singleOf(::AssetProvider)
        }
        val worldModule = module {
            singleOf(::ClientMap)
            singleOf(::TilePool)
            singleOf(::EntityPool)
            singleOf(::Scene)
            singleOf(::Grid)
            factoryOf(::Entity)
        }
        val renderingModule = module {
            singleOf(::VisualManager)
            singleOf(::CameraManager)
            singleOf(::SceneRenderer)
            singleOf(::DebugRenderer)
        }
        val inputModule = module {
            single { InputMultiplexer() }
            singleOf(::InputManager)
            singleOf(::PlayerController)
            singleOf(::GridMovement)
        }
        val koinApp = startKoin {
            slf4jLogger()
            modules(
                coreModule,
                clientModule,
                networkModule,
                bundleModule,
                luaModule,
                dataModule,
                gdxModule,
                worldModule,
                renderingModule,
                inputModule
            )
        }
        koinApp.createEagerInstances()
        listener = koinApp.koin.get<ApplicationListener>()
        listener.create()
    }

    override fun resize(width: Int, height: Int) {
        listener.resize(width, height)
    }

    override fun render() {
        listener.render()
    }

    override fun pause() {
        listener.pause()
    }

    override fun resume() {
        listener.resume()
    }

    override fun dispose() {
        listener.dispose()
    }
}