package world.selene.server

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.sksamuel.hoplite.ConfigLoaderBuilder
import com.sksamuel.hoplite.ExperimentalHoplite
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.jackson.jackson
import org.koin.core.context.startKoin
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.createdAtStart
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

import org.koin.logger.slf4jLogger
import org.slf4j.LoggerFactory
import world.selene.common.bundles.BundleDatabase
import world.selene.common.bundles.BundleLoader
import world.selene.common.bundles.BundleLocator
import world.selene.common.data.ComponentRegistry
import world.selene.common.data.CustomRegistries
import world.selene.common.data.NameIdRegistry
import world.selene.common.lua.LuaManager
import world.selene.common.lua.LuaMixinModule
import world.selene.common.lua.LuaRegistriesModule
import world.selene.common.lua.LuaMixinRegistry
import world.selene.common.lua.LuaModule
import world.selene.common.lua.LuaSchedulesModule
import world.selene.common.threading.MainThreadDispatcher
import world.selene.common.lua.LuaPayloadRegistry
import world.selene.common.lua.LuaResourcesModule
import world.selene.common.lua.LuaHttpModule
import world.selene.common.lua.LuaI18nModule
import world.selene.common.i18n.Messages
import world.selene.server.lua.LuaSavesModule
import world.selene.common.network.PacketFactory
import world.selene.common.network.PacketHandler
import world.selene.common.network.PacketRegistrations
import world.selene.server.bundles.ServerBundleLocator
import world.selene.server.config.ServerConfig
import world.selene.server.config.ScriptProperties
import world.selene.server.data.ServerCustomData
import world.selene.common.data.EntityRegistry
import world.selene.common.data.TileRegistry
import world.selene.common.data.TransitionRegistry
import world.selene.common.grid.Grid
import world.selene.common.lua.LuaGridModule
import world.selene.server.bundles.ClientBundleCache
import world.selene.server.collision.CollisionResolver
import world.selene.server.data.PersistentNameIdRegistry
import world.selene.server.data.Registries
import world.selene.common.data.RegistryProvider
import world.selene.common.data.SoundRegistry
import world.selene.common.util.Disposable
import world.selene.server.dimensions.Dimension
import world.selene.server.dimensions.DimensionManager
import world.selene.server.entities.Entity
import world.selene.server.entities.EntityManager
import world.selene.server.http.HttpServer
import world.selene.server.lua.LuaDimensionsModule
import world.selene.server.lua.LuaEntitiesModule
import world.selene.server.lua.LuaServerMapModule
import world.selene.server.lua.LuaPlayersModule
import world.selene.server.lua.LuaServerModule
import world.selene.server.lua.LuaServerNetworkModule
import world.selene.server.lua.LuaSoundsModule
import world.selene.server.lua.LuaConfigModule
import world.selene.server.lua.ServerLuaSignals
import world.selene.server.login.LoginQueue
import world.selene.server.login.SessionAuthentication
import world.selene.server.lua.Scripting
import world.selene.server.maps.TransitionResolver
import world.selene.server.network.NetworkServer
import world.selene.server.network.NetworkServerImpl
import world.selene.server.network.ServerPacketHandler
import world.selene.server.player.PlayerManager
import world.selene.server.saves.MapTreeFormat
import world.selene.server.saves.MapTreeFormatJsonV1
import world.selene.server.saves.SaveManager
import world.selene.server.sync.ChunkViewManager
import world.selene.server.world.World

val objectMapper = ObjectMapper().registerKotlinModule()

@OptIn(ExperimentalHoplite::class)
fun main(args: Array<String>) {
    ServerConfig.createDefault()
    val coreModule = module {
        single { LoggerFactory.getLogger("Selene") }
    }
    val httpModule = module {
        singleOf(::HttpServer)
        singleOf(::SessionAuthentication)
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
    val luaModule = module {
        singleOf(::LuaManager)
        singleOf(::LuaMixinRegistry)
        singleOf(::LuaPayloadRegistry)
        singleOf(::ServerLuaSignals)
        singleOf(::ServerCustomData)
        singleOf(::Messages)
        singleOf(::LuaServerModule) { bind<LuaModule>() }
        singleOf(::LuaPlayersModule) { bind<LuaModule>() }
        singleOf(::LuaMixinModule) { bind<LuaModule>() }
        singleOf(::LuaServerNetworkModule) { bind<LuaModule>() }
        singleOf(::LuaSoundsModule) { bind<LuaModule>() }
        singleOf(::LuaGridModule) { bind<LuaModule>() }
        singleOf(::LuaResourcesModule) { bind<LuaModule>() }
        singleOf(::LuaSavesModule) { bind<LuaModule>() }
        singleOf(::LuaServerMapModule) { bind<LuaModule>() }
        singleOf(::LuaEntitiesModule) { bind<LuaModule>() }
        singleOf(::LuaDimensionsModule) { bind<LuaModule>() }
        singleOf(::LuaRegistriesModule) { bind<LuaModule>() }
        singleOf(::LuaSchedulesModule) { bind<LuaModule>(); bind<Disposable>() }
        singleOf(::LuaHttpModule) { bind<LuaModule>() }
        singleOf(::LuaConfigModule) { bind<LuaModule>() }
        singleOf(::LuaI18nModule) { bind<LuaModule>() }
        singleOf(::Scripting)
    }
    val bundleModule = module {
        singleOf(::BundleLoader)
        singleOf(::BundleDatabase)
        singleOf(::ClientBundleCache)
        singleOf(::ServerBundleLocator) { bind<BundleLocator>() }
    }
    val networkModule = module {
        singleOf(::NetworkServerImpl) { bind<NetworkServer>() }
        singleOf(::PacketFactory)
        singleOf(::ServerPacketHandler) { bind<PacketHandler<*>>() }
        singleOf(::PacketRegistrations) { createdAtStart() }
        singleOf(::LoginQueue)
    }
    val dataModule = module {
        single { objectMapper }
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
        singleOf(::MapTreeFormatJsonV1) { bind<MapTreeFormat>() }
        // singleOf(::MapTreeFormatBinaryV1) { bind<MapTreeFormat>() }
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
            luaModule,
            httpModule
        )
    }

    koinApp.createEagerInstances()
    val server = koinApp.koin.get<SeleneServer>()
    server.start()
}