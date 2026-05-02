package com.seleneworlds.client.controls

import com.seleneworlds.common.grid.Direction

class MovementGridApi(private val gridMovement: GridMovement) {
    fun setMotion(direction: Direction) {
        gridMovement.moveDirection = direction
    }

    fun setFacing(direction: Direction) {
        gridMovement.facingDirection = direction
    }
}
