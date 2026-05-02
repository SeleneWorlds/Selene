package world.selene.client.controls

import world.selene.common.grid.Direction

class MovementGridApi(private val gridMovement: GridMovement) {
    fun setMotion(direction: Direction) {
        gridMovement.moveDirection = direction
    }

    fun setFacing(direction: Direction) {
        gridMovement.facingDirection = direction
    }
}
