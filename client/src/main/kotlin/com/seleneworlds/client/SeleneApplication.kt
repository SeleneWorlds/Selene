package com.seleneworlds.client

import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.ApplicationListener
import com.badlogic.gdx.InputMultiplexer
import com.sksamuel.hoplite.ExperimentalHoplite
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import ktx.assets.async.AssetStorage
import ktx.async.MainDispatcher
import kotlinx.serialization.json.Json
import org.koin.core.context.startKoin
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module
import org.koin.logger.slf4jLogger
import org.slf4j.LoggerFactory
import com.seleneworlds.client.assets.AssetProvider
import com.seleneworlds.client.assets.RuntimeBundleUpdateManager
import com.seleneworlds.client.bundle.ClientBundleWatcher
import com.seleneworlds.client.bundles.BundleFileResolver
import com.seleneworlds.client.bundles.ClientBundleLocator
import com.seleneworlds.client.camera.CameraApi
import com.seleneworlds.client.camera.CameraLuaApi
import com.seleneworlds.client.camera.CameraManager
import com.seleneworlds.client.config.ClientConfig
import com.seleneworlds.client.config.ClientRuntimeConfig
import com.seleneworlds.client.controls.GridMovement
import com.seleneworlds.client.controls.MovementGridApi
import com.seleneworlds.client.controls.MovementGridLuaApi
import com.seleneworlds.client.controls.PlayerController
import com.seleneworlds.client.data.Registries
import com.seleneworlds.client.entity.EntitiesApi
import com.seleneworlds.client.entity.EntitiesLuaApi
import com.seleneworlds.client.entity.Entity
import com.seleneworlds.client.entity.EntityPool
import com.seleneworlds.client.entity.component.EntityComponentFactory
import com.seleneworlds.client.game.GameApi
import com.seleneworlds.client.game.GameLuaApi
import com.seleneworlds.client.grid.ClientGrid
import com.seleneworlds.client.grid.ClientGridApi
import com.seleneworlds.client.grid.ClientGridLuaApi
import com.seleneworlds.client.input.InputApi
import com.seleneworlds.client.input.InputLuaApi
import com.seleneworlds.client.input.InputManager
import com.seleneworlds.client.maps.ClientMap
import com.seleneworlds.client.maps.MapApi
import com.seleneworlds.client.maps.MapLuaApi
import com.seleneworlds.client.network.*
import com.seleneworlds.client.rendering.DebugRenderer
import com.seleneworlds.client.rendering.SceneRenderer
import com.seleneworlds.client.rendering.drawable.DrawableManager
import com.seleneworlds.client.rendering.environment.Environment
import com.seleneworlds.client.rendering.texture.TexturesApi
import com.seleneworlds.client.rendering.texture.TexturesLuaApi
import com.seleneworlds.client.rendering.scene.Scene
import com.seleneworlds.client.rendering.visual.VisualFactory
import com.seleneworlds.client.rendering.visual.VisualRegistry
import com.seleneworlds.client.rendering.visual.VisualsApi
import com.seleneworlds.client.rendering.visual.VisualsLuaApi
import com.seleneworlds.client.script.ClientLuaScriptProvider
import com.seleneworlds.client.script.ClientScriptProvider
import com.seleneworlds.client.sounds.AudioRegistry
import com.seleneworlds.client.sounds.SoundManager
import com.seleneworlds.client.sounds.SoundsApi
import com.seleneworlds.client.sounds.SoundsLuaApi
import com.seleneworlds.client.tiles.Tile
import com.seleneworlds.client.tiles.TilePool
import com.seleneworlds.client.ui.UI
import com.seleneworlds.client.ui.SkinResolvers
import com.seleneworlds.client.ui.lua.SkinLuaMetatable
import com.seleneworlds.client.ui.UIApi
import com.seleneworlds.client.ui.lua.UILuaApi
import com.seleneworlds.common.bundles.*
import com.seleneworlds.common.data.RegistriesApi
import com.seleneworlds.common.data.RegistriesLuaApi
import com.seleneworlds.common.data.RegistryProvider
import com.seleneworlds.common.data.custom.CustomRegistries
import com.seleneworlds.common.data.mappings.NameIdRegistry
import com.seleneworlds.common.entities.EntityRegistry
import com.seleneworlds.common.entities.component.ComponentRegistry
import com.seleneworlds.common.i18n.I18nApi
import com.seleneworlds.common.i18n.I18nLuaApi
import com.seleneworlds.common.i18n.Messages
import com.seleneworlds.common.jobs.SchedulesApi
import com.seleneworlds.common.jobs.SchedulesLuaApi
import com.seleneworlds.common.lua.LuaManager
import com.seleneworlds.common.lua.LuaModule
import com.seleneworlds.common.lua.libraries.*
import com.seleneworlds.common.network.*
import com.seleneworlds.common.sounds.SoundRegistry
import com.seleneworlds.common.serialization.seleneJson
import com.seleneworlds.common.threading.MainThreadDispatcher
import com.seleneworlds.common.tiles.TileRegistry
import com.seleneworlds.common.util.Disposable

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
        val apiModule = module {
            singleOf(::UIApi)
            singleOf(::VisualsApi)
            singleOf(::NetworkApi)
            singleOf(::CameraApi)
            singleOf(::MapApi)
            singleOf(::SoundsApi)
            singleOf(::TexturesApi)
            singleOf(::InputApi)
            singleOf(::ClientGridApi)
            singleOf(::MovementGridApi)
            singleOf(::GameApi)
            singleOf(::EntitiesApi)
            singleOf(::SchedulesApi)
            singleOf(::HttpApi)
            singleOf(::I18nApi)
            singleOf(::ResourcesApi)
            singleOf(::RegistriesApi)
        }
        val luaModule = module {
            singleOf(::LuaManager)
            single { PayloadHandlerRegistry<Unit>() }
            singleOf(::Messages)
            singleOf(::SkinResolvers)
            singleOf(::SkinLuaMetatable)
            singleOf(::LuaDebugModule) { bind<LuaModule>() }
            singleOf(::LuaOsModule) { bind<LuaModule>() }
            singleOf(::LuaPackageModule) { bind<LuaModule>() }
            singleOf(::LuaMathxModule) { bind<LuaModule>() }
            singleOf(::LuaStringxModule) { bind<LuaModule>() }
            singleOf(::LuaTablexModule) { bind<LuaModule>() }
            singleOf(::UILuaApi) { bind<LuaModule>() }
            singleOf(::VisualsLuaApi) { bind<LuaModule>() }
            singleOf(::NetworkLuaApi) { bind<LuaModule>() }
            singleOf(::CameraLuaApi) { bind<LuaModule>() }
            singleOf(::MapLuaApi) { bind<LuaModule>() }
            singleOf(::SoundsLuaApi) { bind<LuaModule>() }
            singleOf(::TexturesLuaApi) { bind<LuaModule>() }
            singleOf(::ResourcesLuaApi) { bind<LuaModule>() }
            singleOf(::InputLuaApi) { bind<LuaModule>() }
            singleOf(::ClientGridLuaApi) { bind<LuaModule>() }
            singleOf(::MovementGridLuaApi) { bind<LuaModule>() }
            singleOf(::GameLuaApi) { bind<LuaModule>() }
            singleOf(::EntitiesLuaApi) { bind<LuaModule>() }
            singleOf(::RegistriesLuaApi) { bind<LuaModule>() }
            singleOf(::SchedulesLuaApi) { bind<LuaModule>(); bind<Disposable>() }
            singleOf(::HttpLuaApi) { bind<LuaModule>(); bind<Disposable>() }
            singleOf(::I18nLuaApi) { bind<LuaModule>() }
        }
        val bundleModule = module {
            singleOf(::BundleLoader)
            singleOf(::BundleDatabase)
            singleOf(::ClientBundleLocator) { bind<BundleLocator>() }
            singleOf(::ClientBundleWatcher) { bind<Disposable>() }
        }
        val networkModule = module {
            singleOf(::NetworkClientImpl) { bind<NetworkClient>(); bind<Disposable>() }
            singleOf(::PacketFactory)
            singleOf(::ClientPacketHandler) { bind<PacketHandler<*>>() }
            singleOf(::PacketRegistrations)
        }
        val httpModule = module {
            single {
                val json = get<Json>()
                HttpClient(CIO) {
                    install(ContentNegotiation) {
                        json(json)
                    }
                }
            }
        }
        val dataModule = module {
            single { seleneJson }.bind(Json::class)
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
            singleOf(::ClientLuaScriptProvider) { bind<ClientScriptProvider>() }
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
            singleOf(::VisualFactory)
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
                apiModule,
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
