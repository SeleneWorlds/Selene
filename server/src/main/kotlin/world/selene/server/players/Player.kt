package world.selene.server.players

import world.selene.common.lua.*
import world.selene.common.network.packet.SetCameraFollowEntityPacket
import world.selene.common.network.packet.SetCameraPositionPacket
import world.selene.common.network.packet.SetControlledEntityPacket
import world.selene.common.observable.ObservableMap
import world.selene.common.grid.Coordinate
import world.selene.server.cameras.Camera
import world.selene.server.dimensions.Dimension
import world.selene.server.dimensions.DimensionManager
import world.selene.server.entities.Entity
import world.selene.server.network.NetworkClient
import java.util.*

class Player(
    val dimensionManager: DimensionManager,
    private val playerManager: PlayerManager,
    val client: NetworkClient
) : IdResolvable<String, Player> {

    enum class ConnectionState {
        PENDING_AUTHENTICATION,
        PENDING_JOIN,
        READY,
        DISCONNECTED
    }

    val api = PlayerApi(this)

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