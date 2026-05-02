package com.seleneworlds.server

import com.fasterxml.jackson.databind.MapperFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.sksamuel.hoplite.ConfigLoaderBuilder
import com.sksamuel.hoplite.ExperimentalHoplite
import com.sksamuel.hoplite.fp.getOrElse
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.jackson.*
import org.koin.core.context.startKoin
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.createdAtStart
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module
import org.koin.logger.slf4jLogger
import org.slf4j.LoggerFactory
import com.seleneworlds.common.bundles.*
import com.seleneworlds.common.data.RegistriesApi
import com.seleneworlds.common.data.RegistriesLuaApi
import com.seleneworlds.common.data.RegistryProvider
import com.seleneworlds.common.data.custom.CustomRegistries
import com.seleneworlds.common.data.mappings.NameIdRegistry
import com.seleneworlds.common.entities.EntityRegistry
import com.seleneworlds.common.entities.component.ComponentRegistry
import com.seleneworlds.common.grid.Grid
import com.seleneworlds.common.grid.GridApi
import com.seleneworlds.common.grid.GridLuaApi
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
import com.seleneworlds.common.threading.MainThreadDispatcher
import com.seleneworlds.common.tiles.TileRegistry
import com.seleneworlds.common.tiles.transitions.TransitionRegistry
import com.seleneworlds.common.util.Disposable
import com.seleneworlds.server.attributes.AttributesApi
import com.seleneworlds.server.attributes.AttributesLuaApi
import com.seleneworlds.server.bundle.ServerBundleWatcher
import com.seleneworlds.server.bundles.ClientBundleCache
import com.seleneworlds.server.bundles.ServerBundleLocator
import com.seleneworlds.server.collision.CollisionResolver
import com.seleneworlds.server.config.*
import com.seleneworlds.server.data.Registries
import com.seleneworlds.server.data.ServerCustomData
import com.seleneworlds.server.data.mappings.PersistentNameIdRegistry
import com.seleneworlds.server.dimensions.Dimension
import com.seleneworlds.server.dimensions.DimensionManager
import com.seleneworlds.server.dimensions.DimensionsApi
import com.seleneworlds.server.dimensions.DimensionsLuaApi
import com.seleneworlds.server.entities.EntitiesApi
import com.seleneworlds.server.entities.EntitiesLuaApi
import com.seleneworlds.server.entities.Entity
import com.seleneworlds.server.entities.EntityManager
import com.seleneworlds.server.heartbeat.ServerHeartbeat
import com.seleneworlds.server.http.HttpServer
import com.seleneworlds.server.login.LoginQueue
import com.seleneworlds.server.login.SessionAuthentication
import com.seleneworlds.server.maps.ServerMapApi
import com.seleneworlds.server.maps.ServerMapLuaApi
import com.seleneworlds.server.network.*
import com.seleneworlds.server.players.Player
import com.seleneworlds.server.players.PlayerManager
import com.seleneworlds.server.players.PlayersApi
import com.seleneworlds.server.players.PlayersLuaApi
import com.seleneworlds.server.saves.*
import com.seleneworlds.server.sounds.SoundsApi
import com.seleneworlds.server.sounds.SoundsLuaApi
import com.seleneworlds.server.sync.ChunkViewManager
import com.seleneworlds.server.tiles.transitions.TransitionResolver
import com.seleneworlds.server.world.World

val objectMapper = JsonMapper.builder()
    .enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS).build()
    .registerKotlinModule()

@OptIn(ExperimentalHoplite::class)
fun main(args: Array<String>) {
    ServerConfig.createDefault()
    val coreModule = module {
        single { LoggerFactory.getLogger("Selene") }
    }
    val httpModule = module {
        singleOf(::HttpServer)
        singleOf(::SessionAuthentication)
        singleOf(::ServerHeartbeat) { bind<Disposable>() }
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
    val apiModule = module {
        singleOf(::ServerApi)
        singleOf(::PlayersApi)
        singleOf(::NetworkApi)
        singleOf(::SoundsApi)
        singleOf(::ServerMapApi)
        singleOf(::EntitiesApi)
        singleOf(::DimensionsApi)
        singleOf(::ConfigApi)
        singleOf(::AttributesApi)
        singleOf(::SchedulesApi)
        singleOf(::HttpApi)
        singleOf(::I18nApi)
        singleOf(::GridApi)
        singleOf(::ResourcesApi)
        singleOf(::RegistriesApi)
        singleOf(::SavesApi)
    }
    val luaModule = module {
        singleOf(::LuaManager)
        single { PayloadHandlerRegistry<Player>() }
        singleOf(::ServerCustomData)
        singleOf(::Messages)
        singleOf(::LuaDebugModule) { bind<LuaModule>() }
        singleOf(::LuaOsModule) { bind<LuaModule>() }
        singleOf(::LuaPackageModule) { bind<LuaModule>() }
        singleOf(::LuaMathxModule) { bind<LuaModule>() }
        singleOf(::LuaStringxModule) { bind<LuaModule>() }
        singleOf(::LuaTablexModule) { bind<LuaModule>() }
        singleOf(::ServerLuaApi) { bind<LuaModule>() }
        singleOf(::PlayersLuaApi) { bind<LuaModule>() }
        singleOf(::NetworkLuaApi) { bind<LuaModule>() }
        singleOf(::SoundsLuaApi) { bind<LuaModule>() }
        singleOf(::GridLuaApi) { bind<LuaModule>() }
        singleOf(::ResourcesLuaApi) { bind<LuaModule>() }
        singleOf(::SavesLuaApi) { bind<LuaModule>() }
        singleOf(::ServerMapLuaApi) { bind<LuaModule>() }
        singleOf(::EntitiesLuaApi) { bind<LuaModule>() }
        singleOf(::DimensionsLuaApi) { bind<LuaModule>() }
        singleOf(::RegistriesLuaApi) { bind<LuaModule>() }
        singleOf(::SchedulesLuaApi) { bind<LuaModule>(); bind<Disposable>() }
        singleOf(::HttpLuaApi) { bind<LuaModule>(); bind<Disposable>() }
        singleOf(::ConfigLuaApi) { bind<LuaModule>() }
        singleOf(::I18nLuaApi) { bind<LuaModule>() }
        singleOf(::AttributesLuaApi) { bind<LuaModule>() }
    }
    val bundleModule = module {
        singleOf(::BundleLoader)
        singleOf(::BundleDatabase)
        singleOf(::ClientBundleCache)
        singleOf(::ServerBundleLocator) { bind<BundleLocator>() }
        singleOf(::ServerBundleWatcher) { bind<Disposable>() }
    }
    val networkModule = module {
        singleOf(::NetworkServerImpl) { bind<NetworkServer>() }
        singleOf(::PacketFactory)
        singleOf(::ServerPacketHandler) { bind<PacketHandler<*>>() }
        singleOf(::PacketRegistrations) { createdAtStart() }
        singleOf(::LoginQueue)
    }
    val dataModule = module {
        single { objectMapper }.bind(ObjectMapper::class)
        singleOf(::TileRegistry)
        singleOf(::TransitionRegistry)
        singleOf(::EntityRegistry)
        singleOf(::ComponentRegistry)
        singleOf(::SoundRegistry)
        singleOf(::CustomRegistries)
        singleOf(::PersistentNameIdRegistry) { bind<NameIdRegistry>() }
        singleOf(::Registries) { bind<RegistryProvider>() }
    }
    val worldModule = module {
        singleOf(::MapTreeFormatJsonV1)
        singleOf(::MapTreeFormatBinaryV1) { bind<MapTreeFormat>() }
        singleOf(::SaveManager)
        singleOf(::DimensionManager)
        singleOf(::EntityManager)
        singleOf(::PlayerManager)
        singleOf(::ChunkViewManager)
        singleOf(::TransitionResolver)
        singleOf(::CollisionResolver)
        singleOf(::Grid)
        singleOf(::World)
        factoryOf(::Entity)
        factoryOf(::Dimension)
    }
    val serverModule = module {
        single {
            ConfigLoaderBuilder.default()
                .withExplicitSealedTypes()
                .build()
                .loadConfigOrThrow<ServerConfig>("server.properties")
        }
        single {
            ConfigLoaderBuilder.default()
                .withExplicitSealedTypes()
                .build()
                .loadConfig<SystemConfig>("system.properties")
                .getOrElse { SystemConfig() }
        }
        singleOf(::ScriptProperties)
        singleOf(::MainThreadDispatcher)
        singleOf(::SeleneServer)
    }
    val koinApp = startKoin {
        slf4jLogger()
        modules(
            coreModule,
            serverModule,
            networkModule,
            bundleModule,
            worldModule,
            dataModule,
            apiModule,
            luaModule,
            httpModule
        )
    }

    koinApp.createEagerInstances()
    val server = koinApp.koin.get<SeleneServer>()
    server.start()
}
