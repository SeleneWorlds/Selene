package world.selene.client.controls

import world.selene.client.maps.Entity
import world.selene.client.network.NetworkClient
import world.selene.common.grid.Grid
import world.selene.common.network.packet.RequestMovePacket

class GridMovement(
    private val playerController: PlayerController,
    private val networkClient: NetworkClient
) {
    var moveDirection: Grid.Direction? = null
    var requestedStep: Boolean = false

    private val entity get() = playerController.controlledEntity

    fun update(delta: Float) {
        val entity = playerController.controlledEntity
        moveDirection?.let { moveDirection ->
            if (entity != null && canRequestNextMove(entity)) {
                networkClient.send(RequestMovePacket(entity.coordinate + moveDirection.vector))
                requestedStep = true
            }
        }
        moveDirection = null
    }


    fun canRequestNextMove(entity: Entity): Boolean {
        if (requestedStep) {
            return false
        }

        return entity.motionQueue.isEmpty()
    }

    fun confirmMove() {
        requestedStep = false
    }
}