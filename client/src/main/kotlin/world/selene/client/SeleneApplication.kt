package world.selene.client

import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.ApplicationListener
import com.badlogic.gdx.InputMultiplexer
import com.fasterxml.jackson.databind.ObjectMapper
import com.sksamuel.hoplite.ExperimentalHoplite
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.jackson.*
import ktx.assets.async.AssetStorage
import ktx.async.MainDispatcher
import org.koin.core.context.startKoin
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module
import org.koin.logger.slf4jLogger
import org.slf4j.LoggerFactory
import world.selene.client.assets.AssetProvider
import world.selene.client.assets.RuntimeBundleUpdateManager
import world.selene.client.bundles.BundleFileResolver
import world.selene.client.bundles.ClientBundleLocator
import world.selene.client.camera.CameraManager
import world.selene.client.camera.LuaCameraModule
import world.selene.client.config.ClientConfig
import world.selene.client.config.ClientRuntimeConfig
import world.selene.client.controls.GridMovement
import world.selene.client.controls.LuaMovementGridModule
import world.selene.client.controls.PlayerController
import world.selene.client.sounds.AudioRegistry
import world.selene.client.data.Registries
import world.selene.client.entity.Entity
import world.selene.client.entity.EntityPool
import world.selene.client.entity.LuaEntitiesModule
import world.selene.client.rendering.visual.VisualRegistry
import world.selene.client.entity.component.EntityComponentFactory
import world.selene.client.grid.ClientGrid
import world.selene.client.grid.LuaClientGridModule
import world.selene.client.input.InputManager
import world.selene.client.input.LuaInputModule
import world.selene.client.lua.*
import world.selene.client.maps.*
import world.selene.client.network.ClientPacketHandler
import world.selene.client.network.LuaClientNetworkModule
import world.selene.client.network.NetworkClient
import world.selene.client.network.NetworkClientImpl
import world.selene.client.rendering.DebugRenderer
import world.selene.client.rendering.SceneRenderer
import world.selene.client.rendering.drawable.DrawableManager
import world.selene.client.rendering.environment.Environment
import world.selene.client.rendering.lua.LuaTexturesModule
import world.selene.client.rendering.visual.LuaVisualsModule
import world.selene.client.rendering.visual.VisualManager
import world.selene.client.rendering.scene.Scene
import world.selene.client.sounds.LuaSoundsModule
import world.selene.client.sounds.SoundManager
import world.selene.client.tiles.Tile
import world.selene.client.tiles.TilePool
import world.selene.client.ui.UI
import world.selene.client.ui.lua.LuaSkinUtils
import world.selene.client.ui.lua.LuaUIModule
import world.selene.client.ui.lua.SkinLuaMetatable
import world.selene.common.bundles.BundleDatabase
import world.selene.common.bundles.BundleLoader
import world.selene.common.bundles.BundleLocator
import world.selene.common.bundles.LuaResourcesModule
import world.selene.common.data.*
import world.selene.common.data.custom.CustomRegistries
import world.selene.common.data.mappings.NameIdRegistry
import world.selene.common.entities.EntityRegistry
import world.selene.common.entities.component.ComponentRegistry
import world.selene.common.i18n.LuaI18nModule
import world.selene.common.i18n.Messages
import world.selene.common.jobs.LuaSchedulesModule
import world.selene.common.lua.*
import world.selene.common.lua.libraries.LuaDebugModule
import world.selene.common.lua.libraries.LuaMathxModule
import world.selene.common.lua.libraries.LuaOsModule
import world.selene.common.lua.libraries.LuaPackageModule
import world.selene.common.lua.libraries.LuaStringxModule
import world.selene.common.lua.libraries.LuaTablexModule
import world.selene.common.network.LuaHttpModule
import world.selene.common.network.LuaPayloadRegistry
import world.selene.common.network.PacketFactory
import world.selene.common.network.PacketHandler
import world.selene.common.network.PacketRegistrations
import world.selene.common.sounds.SoundRegistry
import world.selene.common.threading.MainThreadDispatcher
import world.selene.common.tiles.TileRegistry
import world.selene.common.util.Disposable

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
            singleOf(::LuaPayloadRegistry)
            singleOf(::ClientLuaSignals)
            singleOf(::Messages)
            singleOf(::LuaSkinUtils)
            singleOf(::SkinLuaMetatable)
            singleOf(::LuaDebugModule) { bind<LuaModule>() }
            singleOf(::LuaOsModule) { bind<LuaModule>() }
            singleOf(::LuaPackageModule) { bind<LuaModule>() }
            singleOf(::LuaMathxModule) { bind<LuaModule>() }
            singleOf(::LuaStringxModule) { bind<LuaModule>() }
            singleOf(::LuaTablexModule) { bind<LuaModule>() }
            singleOf(::LuaUIModule) { bind<LuaModule>() }
            singleOf(::LuaVisualsModule) { bind<LuaModule>() }
            singleOf(::LuaClientNetworkModule) { bind<LuaModule>() }
            singleOf(::LuaCameraModule) { bind<LuaModule>() }
            singleOf(::LuaClientMapModule) { bind<LuaModule>() }
            singleOf(::LuaSoundsModule) { bind<LuaModule>() }
            singleOf(::LuaTexturesModule) { bind<LuaModule>() }
            singleOf(::LuaResourcesModule) { bind<LuaModule>() }
            singleOf(::LuaInputModule) { bind<LuaModule>() }
            singleOf(::LuaClientGridModule) { bind<LuaModule>() }
            singleOf(::LuaMovementGridModule) { bind<LuaModule>() }
            singleOf(::LuaGameModule) { bind<LuaModule>() }
            singleOf(::LuaEntitiesModule) { bind<LuaModule>() }
            singleOf(::LuaRegistriesModule) { bind<LuaModule>() }
            singleOf(::LuaSchedulesModule) { bind<LuaModule>(); bind<Disposable>() }
            singleOf(::LuaHttpModule) { bind<LuaModule>(); bind<Disposable>() }
            singleOf(::LuaI18nModule) { bind<LuaModule>() }
        }
        val bundleModule = module {
            singleOf(::BundleLoader)
            singleOf(::BundleDatabase)
            singleOf(::ClientBundleLocator) { bind<BundleLocator>() }
        }
        val networkModule = module {
            singleOf(::NetworkClientImpl) { bind<NetworkClient>(); bind<Disposable>() }
            singleOf(::PacketFactory)
            singleOf(::ClientPacketHandler) { bind<PacketHandler<*>>() }
            singleOf(::PacketRegistrations)
        }
        val httpModule = module {
            single {
                val objectMapper = get<ObjectMapper>()
                HttpClient(CIO) {
                    install(ContentNegotiation) {
                        jackson {
                            setConfig(objectMapper.serializationConfig)
                            setConfig(objectMapper.deserializationConfig)
                        }
                    }
                }
            }
        }
        val dataModule = module {
            single { objectMapper }.bind(ObjectMapper::class)
            singleOf(::TileRegistry)
            singleOf(::EntityRegistry)
            singleOf(::ComponentRegistry)
            singleOf(::SoundRegistry)
            singleOf(::VisualRegistry)
            singleOf(::AudioRegistry)
            singleOf(::CustomRegistries)
            singleOf(::NameIdRegistry)
            singleOf(::Registries) { bind<RegistryProvider>() }
        }
        val clientModule = module {
            single { config }
            single { runtimeConfig }
            singleOf(::UI)
            singleOf(::MainThreadDispatcher)
            singleOf(::SeleneClient)
            singleOf(::RuntimeBundleUpdateManager) { bind<Disposable>() }
        }
        val gdxModule = module {
            singleOf(::SeleneApplicationListener) { bind<ApplicationListener>() }
            singleOf(::BundleFileResolver)
            single { AssetStorage(fileResolver = get<BundleFileResolver>()) }
            singleOf(::AssetProvider) { bind<Disposable>() }
            singleOf(::DrawableManager) { bind<Disposable>() }
        }
        val worldModule = module {
            singleOf(::ClientMap)
            singleOf(::TilePool)
            singleOf(::EntityPool)
            singleOf(::Scene)
            singleOf(::ClientGrid)
            singleOf(::EntityComponentFactory)
            factoryOf(::Tile)
            factoryOf(::Entity)
        }
        val renderingModule = module {
            singleOf(::VisualManager)
            singleOf(::CameraManager)
            singleOf(::SceneRenderer)
            singleOf(::Environment)
            singleOf(::DebugRenderer)
        }
        val audioModule = module {
            singleOf(::SoundManager) { bind<Disposable>() }
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
                audioModule,
                inputModule,
                httpModule
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