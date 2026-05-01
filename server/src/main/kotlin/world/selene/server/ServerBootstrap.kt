package world.selene.server

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
import world.selene.common.bundles.BundleDatabase
import world.selene.common.bundles.BundleLoader
import world.selene.common.bundles.BundleLocator
import world.selene.common.bundles.ResourcesApi
import world.selene.common.bundles.ResourcesLuaApi
import world.selene.common.data.*
import world.selene.common.data.custom.CustomRegistries
import world.selene.common.data.mappings.NameIdRegistry
import world.selene.common.entities.EntityRegistry
import world.selene.common.entities.component.ComponentRegistry
import world.selene.common.grid.Grid
import world.selene.common.grid.GridApi
import world.selene.common.grid.GridLuaApi
import world.selene.common.i18n.I18nApi
import world.selene.common.i18n.I18nLuaApi
import world.selene.common.i18n.Messages
import world.selene.common.jobs.SchedulesApi
import world.selene.common.jobs.SchedulesLuaApi
import world.selene.common.lua.*
import world.selene.common.lua.libraries.LuaDebugModule
import world.selene.common.lua.libraries.LuaMathxModule
import world.selene.common.lua.libraries.LuaOsModule
import world.selene.common.lua.libraries.LuaPackageModule
import world.selene.common.lua.libraries.LuaStringxModule
import world.selene.common.lua.libraries.LuaTablexModule
import world.selene.common.network.HttpApi
import world.selene.common.network.HttpLuaApi
import world.selene.common.network.LuaPayloadRegistry
import world.selene.common.network.PacketFactory
import world.selene.common.network.PacketHandler
import world.selene.common.network.PacketRegistrations
import world.selene.common.sounds.SoundRegistry
import world.selene.common.threading.MainThreadDispatcher
import world.selene.common.tiles.TileRegistry
import world.selene.common.tiles.transitions.TransitionRegistry
import world.selene.common.util.Disposable
import world.selene.server.attributes.AttributesApi
import world.selene.server.attributes.AttributesLuaApi
import world.selene.server.bundle.ServerBundleWatcher
import world.selene.server.bundles.ClientBundleCache
import world.selene.server.bundles.ServerBundleLocator
import world.selene.server.collision.CollisionResolver
import world.selene.server.config.ConfigApi
import world.selene.server.config.ConfigLuaApi
import world.selene.server.config.ScriptProperties
import world.selene.server.config.ServerConfig
import world.selene.server.config.SystemConfig
import world.selene.server.heartbeat.ServerHeartbeat
import world.selene.server.data.mappings.PersistentNameIdRegistry
import world.selene.server.data.Registries
import world.selene.server.data.ServerCustomData
import world.selene.server.dimensions.Dimension
import world.selene.server.dimensions.DimensionManager
import world.selene.server.dimensions.DimensionsApi
import world.selene.server.dimensions.DimensionsLuaApi
import world.selene.server.entities.Entity
import world.selene.server.entities.EntityManager
import world.selene.server.entities.EntitiesApi
import world.selene.server.entities.EntitiesLuaApi
import world.selene.server.http.HttpServer
import world.selene.server.login.LoginQueue
import world.selene.server.login.SessionAuthentication
import world.selene.server.lua.*
import world.selene.server.maps.ServerMapApi
import world.selene.server.maps.ServerMapLuaApi
import world.selene.server.tiles.transitions.TransitionResolver
import world.selene.server.network.NetworkApi
import world.selene.server.network.NetworkLuaApi
import world.selene.server.network.NetworkServer
import world.selene.server.network.NetworkServerImpl
import world.selene.server.network.ServerPacketHandler
import world.selene.server.players.PlayersApi
import world.selene.server.players.PlayersLuaApi
import world.selene.server.players.PlayerManager
import world.selene.server.saves.SavesLuaApi
import world.selene.server.saves.MapTreeFormat
import world.selene.server.saves.MapTreeFormatBinaryV1
import world.selene.server.saves.MapTreeFormatJsonV1
import world.selene.server.saves.SaveManager
import world.selene.server.saves.SavesApi
import world.selene.server.sounds.SoundsApi
import world.selene.server.sounds.SoundsLuaApi
import world.selene.server.sync.ChunkViewManager
import world.selene.server.world.World

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
        singleOf(::LuaPayloadRegistry)
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
