package world.selene.client.controls

import world.selene.client.maps.Entity
import world.selene.client.network.NetworkClient
import world.selene.common.grid.Direction
import world.selene.common.network.packet.RequestFacingPacket
import world.selene.common.network.packet.RequestMovePacket

class GridMovement(
    private val playerController: PlayerController,
    private val networkClient: NetworkClient
) {
    var moveDirection: Direction? = null
    var facingDirection: Direction? = null
    var requestedStep: Boolean = false

    fun update() {
        val entity = playerController.controlledEntity
        moveDirection?.let { moveDirection ->
            if (entity != null && canRequestNextMove(entity)) {
                networkClient.send(RequestMovePacket(entity.coordinate + moveDirection.vector))
                requestedStep = true
            }
        }
        facingDirection?.let { facingDirection ->
            if (entity != null && entity.facing != facingDirection.angle) {
                networkClient.send(RequestFacingPacket(facingDirection.angle))
            }
        }
        moveDirection = null
        facingDirection = null
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