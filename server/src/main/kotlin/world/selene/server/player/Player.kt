package world.selene.server.player

import party.iroiro.luajava.Lua
import world.selene.common.lua.checkInt
import world.selene.common.lua.checkJavaObject
import world.selene.common.network.packet.SetControlledEntityPacket
import world.selene.common.network.packet.SetCameraFollowEntityPacket
import world.selene.common.network.packet.SetCameraPositionPacket
import world.selene.common.util.Coordinate
import world.selene.server.cameras.Camera
import world.selene.server.dimensions.Dimension
import world.selene.server.entities.Entity
import world.selene.server.network.NetworkClient

class Player(val playerManager: PlayerManager, val client: NetworkClient) {

    val syncManager = playerManager.createSyncManager(this)
    val camera = Camera().apply {
        addListener(syncManager)
    }
    val luaProxy = PlayerLuaProxy(this)
    var controlledEntity: Entity? = null
    var cameraEntity: Entity? = null

    fun update() {
        camera.update()
        syncManager.update()
    }

    class PlayerLuaProxy(val delegate: Player) {
        fun SetCameraToFollowControlledEntity() {
            delegate.controlledEntity?.let { entity ->
                delegate.camera.followEntity(entity)
            }
            delegate.client.send(SetCameraFollowEntityPacket(delegate.controlledEntity?.networkId ?: -1))
        }

        fun SetCameraToFollowTarget() {
            delegate.cameraEntity?.let { entity ->
                delegate.camera.followEntity(entity)
            }
            delegate.client.send(SetCameraFollowEntityPacket(delegate.cameraEntity?.networkId ?: -1))
        }

        fun SetCameraToCoordinate(lua: Lua): Int {
            val dimension = lua.checkJavaObject<Dimension.DimensionLuaProxy>(1).delegate
            val x = lua.checkInt(2)
            val y = lua.checkInt(3)
            val z = lua.checkInt(4)
            delegate.camera.focusCoordinate(dimension, Coordinate(x, y, z))
            delegate.client.send(SetCameraPositionPacket(delegate.camera.coordinate))
            return 0
        }

        fun GetControlledEntity(): Entity.EntityLuaProxy? {
            return delegate.controlledEntity?.luaProxy
        }

        fun SetControlledEntity(entity: Entity.EntityLuaProxy?) {
            delegate.controlledEntity = entity?.delegate
            delegate.client.send(SetControlledEntityPacket(delegate.controlledEntity?.networkId ?: -1))
        }

        fun GetCameraEntity(): Entity.EntityLuaProxy? {
            return delegate.cameraEntity?.luaProxy
        }

        fun SetCameraEntity(entity: Entity.EntityLuaProxy?) {
            delegate.cameraEntity = entity?.delegate
        }
    }
}