package world.selene.client.controls

import world.selene.client.maps.Entity
import world.selene.client.network.NetworkClient
import world.selene.common.network.packet.RequestMovePacket
import world.selene.common.util.Coordinate

class GridMovement(
    private val playerController: PlayerController,
    private val networkClient: NetworkClient
) {
    var moveDirection: Coordinate = Coordinate.Zero
    var requestedStep: Boolean = false

    private val entity get() = playerController.controlledEntity

    fun update(delta: Float) {
        val entity = playerController.controlledEntity
        if (moveDirection != Coordinate.Zero && entity != null && canRequestNextMove(entity)) {
            networkClient.send(RequestMovePacket(entity.coordinate + moveDirection))
            requestedStep = true
        }
        moveDirection = Coordinate.Zero
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