package com.seleneworlds.client.controls

import com.seleneworlds.client.entity.Entity
import com.seleneworlds.client.network.NetworkClient
import com.seleneworlds.common.grid.Direction
import com.seleneworlds.common.network.packet.RequestFacingPacket
import com.seleneworlds.common.network.packet.RequestMovePacket

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