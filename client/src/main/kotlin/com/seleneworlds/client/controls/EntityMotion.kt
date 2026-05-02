package com.seleneworlds.client.controls

import com.seleneworlds.common.grid.Coordinate

class EntityMotion(val start: Coordinate, val end: Coordinate, val duration: Float) {
    var timePassed = 0f
}