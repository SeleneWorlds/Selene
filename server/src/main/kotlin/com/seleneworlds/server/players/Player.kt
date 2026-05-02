package com.seleneworlds.server.players

import com.seleneworlds.common.network.packet.SetCameraFollowEntityPacket
import com.seleneworlds.common.network.packet.SetCameraPositionPacket
import com.seleneworlds.common.network.packet.SetControlledEntityPacket
import com.seleneworlds.common.observable.ObservableMap
import com.seleneworlds.common.grid.Coordinate
import com.seleneworlds.common.script.ExposedApi
import com.seleneworlds.common.util.IdResolvable
import com.seleneworlds.common.util.ResolvableReference
import com.seleneworlds.server.cameras.Camera
import com.seleneworlds.server.dimensions.Dimension
import com.seleneworlds.server.dimensions.DimensionManager
import com.seleneworlds.server.entities.Entity
import com.seleneworlds.server.network.NetworkClient
import java.util.*

class Player(
    val dimensionManager: DimensionManager,
    private val playerManager: PlayerManager,
    val client: NetworkClient
) : IdResolvable<String, Player>, ExposedApi<PlayerApi> {

    enum class ConnectionState {
        PENDING_AUTHENTICATION,
        PENDING_JOIN,
        READY,
        DISCONNECTED
    }

    override val api = PlayerApi(this)

    var connectionState: ConnectionState = ConnectionState.PENDING_AUTHENTICATION
    var userId: String? = null
    var locale: Locale = Locale.ENGLISH
    val localeString get() = locale.toString()
    val languageString: String get() = locale.language
    val customData = ObservableMap()

    override fun resolvableReference(): ResolvableReference<String, Player> {
        return ResolvableReference(Player::class, userId!!, playerManager)
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

}
