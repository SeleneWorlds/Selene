package world.selene.server.player

import party.iroiro.luajava.Lua
import world.selene.common.lua.LuaMappedMetatable
import world.selene.common.lua.LuaMetatable
import world.selene.common.lua.LuaMetatableProvider
import world.selene.common.lua.LuaReferencable
import world.selene.common.lua.LuaReference
import world.selene.common.lua.checkInt
import world.selene.common.lua.checkUserdata
import world.selene.common.lua.checkString
import world.selene.common.network.packet.SetControlledEntityPacket
import world.selene.common.network.packet.SetCameraFollowEntityPacket
import world.selene.common.network.packet.SetCameraPositionPacket
import world.selene.common.util.Coordinate
import world.selene.server.cameras.Camera
import world.selene.server.dimensions.Dimension
import world.selene.server.entities.Entity
import world.selene.server.network.NetworkClient
import java.util.Locale

class Player(private val playerManager: PlayerManager, val client: NetworkClient) : LuaMetatableProvider,
    LuaReferencable<String, Player> {

    var userId: String? = null
    var locale: Locale = Locale.ENGLISH
    val localeString get() = locale.toString()
    val languageString: String get() = locale.language
    val customData = mutableMapOf<String, Any>()

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

    fun setCameraToCoordinate(lua: Lua): Int {
        val dimension = lua.checkUserdata<Dimension>(1)
        val x = lua.checkInt(2)
        val y = lua.checkInt(3)
        val z = lua.checkInt(4)
        camera.focusCoordinate(dimension, Coordinate(x, y, z))
        client.send(SetCameraPositionPacket(camera.coordinate))
        return 0
    }

    val idleTime get() = ((System.currentTimeMillis() - lastInputTime) / 1000L).toInt()

    override fun luaMetatable(lua: Lua): LuaMetatable {
        return luaMeta
    }

    companion object {
        val luaMeta = LuaMappedMetatable(Player::class) {
            readOnly(Player::idleTime)
            readOnly(Player::userId)
            readOnly(Player::localeString, "Locale")
            readOnly(Player::languageString, "Language")
            writable(Player::controlledEntity)
            writable(Player::cameraEntity)
            callable(Player::setCameraToFollowControlledEntity)
            callable(Player::setCameraToFollowTarget)
            callable(Player::setCameraToCoordinate)
            callable("Ref") {
                it.push(it.checkSelf().luaReference(), Lua.Conversion.NONE)
                1
            }
            callable("GetCustomData") {
                val player = it.checkSelf()
                val key = it.checkString(2)
                val defaultValue = it.toObject(3)
                val value = player.customData.getOrDefault(key, defaultValue)
                it.push(value, Lua.Conversion.FULL)
                1
            }
            callable("SetCustomData") {
                val player = it.checkSelf()
                val key = it.checkString(2)
                val value = it.toObject(3)!!
                player.customData[key] = value
                0
            }
        }
    }
}