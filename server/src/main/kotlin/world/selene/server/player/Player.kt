package world.selene.server.player

import party.iroiro.luajava.Lua
import world.selene.common.lua.*
import world.selene.common.network.packet.SetCameraFollowEntityPacket
import world.selene.common.network.packet.SetCameraPositionPacket
import world.selene.common.network.packet.SetControlledEntityPacket
import world.selene.common.util.Coordinate
import world.selene.server.cameras.Camera
import world.selene.server.dimensions.Dimension
import world.selene.server.dimensions.DimensionManager
import world.selene.server.entities.Entity
import world.selene.server.network.NetworkClient
import java.util.*

class Player(
    private val dimensionManager: DimensionManager,
    private val playerManager: PlayerManager,
    val client: NetworkClient
) : LuaMetatableProvider,
    LuaReferencable<String, Player> {

    enum class ConnectionState {
        PENDING_AUTHENTICATION,
        PENDING_JOIN,
        READY
    }

    var connectionState: ConnectionState = ConnectionState.PENDING_AUTHENTICATION
    var userId: String? = null
    var locale: Locale = Locale.ENGLISH
    val localeString get() = locale.toString()
    val languageString: String get() = locale.language
    val customData = ObservableMap()

    override fun luaReference(): LuaReference<String, Player> {
        return LuaReference(Player::class, userId!!, playerManager)
    }

    val syncManager = playerManager.createSyncManager(this)
    val camera = Camera().apply {
        addListener(syncManager)
    }
    var controlledEntity: Entity? = null
        set(value) {
            if (field != value) {
                field = value
                client.send(SetControlledEntityPacket(value?.networkId ?: -1))
            }
        }
    var cameraEntity: Entity? = null

    var lastInputTime = System.currentTimeMillis()

    fun resetLastInputTime() {
        lastInputTime = System.currentTimeMillis()
    }

    fun update() {
        camera.update()
        syncManager.update()
    }

    fun setCameraToFollowControlledEntity() {
        controlledEntity?.let { entity ->
            camera.followEntity(entity)
        }
        client.send(SetCameraFollowEntityPacket(controlledEntity?.networkId ?: -1))
    }

    fun setCameraToFollowTarget() {
        cameraEntity?.let { entity ->
            camera.followEntity(entity)
        }
        client.send(SetCameraFollowEntityPacket(cameraEntity?.networkId ?: -1))
    }

    fun setCameraToCoordinate(dimension: Dimension, coordinate: Coordinate) {
        camera.focusCoordinate(dimension, coordinate)
        client.send(SetCameraPositionPacket(camera.coordinate))
    }

    val idleTime get() = ((System.currentTimeMillis() - lastInputTime) / 1000L).toInt()

    override fun luaMetatable(lua: Lua): LuaMetatable {
        return luaMeta
    }

    @Suppress("SameReturnValue")
    companion object {
        /**
         * Observable map for storing data on this player.
         *
         * ```property
         * CustomData: ObservableMap
         * ```
         */
        private fun luaGetCustomData(lua: Lua): Int {
            val player = lua.checkUserdata<Player>(1)
            lua.push(player.customData, Lua.Conversion.NONE)
            return 1
        }

        /**
         * Seconds since the server last received input from this player.
         *
         * ```property
         * IdleTime: number
         * ```
         */
        private fun luaGetIdleTime(lua: Lua): Int {
            val player = lua.checkUserdata<Player>(1)
            lua.push(player.idleTime)
            return 1
        }

        /**
         * Unique identifier for this player.
         *
         * ```property
         * UserId: string
         * ```
         */
        private fun luaGetUserId(lua: Lua): Int {
            val player = lua.checkUserdata<Player>(1)
            lua.push(player.userId, Lua.Conversion.FULL)
            return 1
        }

        /**
         * Locale tag of this player, e.g. `"en_US"`.
         *
         * ```property
         * LocaleString: string
         * ```
         */
        private fun luaGetLocaleString(lua: Lua): Int {
            val player = lua.checkUserdata<Player>(1)
            lua.push(player.localeString)
            return 1
        }

        /**
         * Language tag of this player, e.g. `"en"`.
         *
         * ```property
         * LanguageString: string
         * ```
         */
        private fun luaGetLanguageString(lua: Lua): Int {
            val player = lua.checkUserdata<Player>(1)
            lua.push(player.languageString)
            return 1
        }

        /**
         * Entity controlled by this player or `nil`.
         *
         * ```property
         * ControlledEntity: Entity|nil
         * ```
         */
        private fun luaGetControlledEntity(lua: Lua): Int {
            val player = lua.checkUserdata<Player>(1)
            lua.push(player.controlledEntity, Lua.Conversion.NONE)
            return 1
        }

        /**
         * ```property
         * ControlledEntity: Entity
         * ```
         */
        private fun luaSetControlledEntity(lua: Lua): Int {
            val player = lua.checkUserdata<Player>(1)
            player.controlledEntity = lua.checkUserdata<Entity>(3)
            return 0
        }

        /**
         * Entity backing this player's vision or `nil`.
         *
         * ```property
         * CameraEntity: Entity|nil
         * ```
         */
        private fun luaGetCameraEntity(lua: Lua): Int {
            val player = lua.checkUserdata<Player>(1)
            lua.push(player.cameraEntity, Lua.Conversion.NONE)
            return 1
        }

        /**
         * ```property
         * CameraEntity: Entity
         * ```
         */
        private fun luaSetCameraEntity(lua: Lua): Int {
            val player = lua.checkUserdata<Player>(1)
            player.cameraEntity = lua.checkUserdata<Entity>(3)
            return 0
        }

        /**
         * Sets the camera to follow the player's controlled entity.
         *
         * ```signatures
         * SetCameraToFollowControlledEntity()
         * ```
         */
        private fun luaSetCameraToFollowControlledEntity(lua: Lua): Int {
            val player = lua.checkUserdata<Player>(1)
            player.setCameraToFollowControlledEntity()
            return 0
        }

        /**
         * Sets the camera to follow the player's camera target entity.
         *
         * ```signatures
         * SetCameraToFollowTarget()
         * ```
         */
        private fun luaSetCameraToFollowTarget(lua: Lua): Int {
            val player = lua.checkUserdata<Player>(1)
            player.setCameraToFollowTarget()
            return 0
        }

        /**
         * Sets the camera to focus on a specific coordinate in a dimension.
         *
         * ```signatures
         * SetCameraToCoordinate(coordinate: Coordinate, dimension: Dimension|nil)
         * SetCameraToCoordinate(x: number, y: number, z: number, dimension: Dimension|nil)
         * ```
         */
        private fun luaSetCameraToCoordinate(lua: Lua): Int {
            val player = lua.checkUserdata<Player>(1)
            val (coordinate, index) = lua.checkCoordinate(2)
            val dimension = lua.toUserdata<Dimension>(index + 1)
                ?: player.camera.dimension ?: player.dimensionManager.getOrCreateDimension(0)
            player.setCameraToCoordinate(dimension, coordinate)
            return 0
        }

        /**
         * Gets a safe reference to this player that provides access to a player without the risk of memory leaks.
         * Player references are backed by the player id. If a player rejoins under a new session, the reference will
         * update to point to their new Player object.
         *
         * ```signatures
         * Ref() -> LuaReference
         * ```
         */
        private fun luaRef(lua: Lua): Int {
            val player = lua.checkUserdata<Player>(1)
            lua.push(player.luaReference(), Lua.Conversion.NONE)
            return 1
        }

        val luaMeta = LuaMappedMetatable(Player::class) {
            getter(::luaGetCustomData)
            getter(::luaGetIdleTime)
            getter(::luaGetUserId)
            getter(::luaGetLocaleString, "Locale")
            getter(::luaGetLanguageString, "Language")
            getter(::luaGetControlledEntity)
            setter(::luaSetControlledEntity)
            getter(::luaGetCameraEntity)
            setter(::luaSetCameraEntity)
            callable(::luaSetCameraToFollowControlledEntity)
            callable(::luaSetCameraToFollowTarget)
            callable(::luaSetCameraToCoordinate)
            callable(::luaRef)
        }
    }
}