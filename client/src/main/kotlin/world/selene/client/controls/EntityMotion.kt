package world.selene.client.controls

import world.selene.common.util.Coordinate

class EntityMotion(val start: Coordinate, val end: Coordinate, val duration: Float) {
    var timePassed = 0f
}