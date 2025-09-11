package world.selene.common.grid

data class Direction(val name: String, val vector: Coordinate, val angle: Float = 0f) {
    companion object {
        val None = Direction("none", Coordinate(0, 0, 0), 0f)
    }
}