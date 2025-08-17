package world.selene.server.maps

import world.selene.common.util.Coordinate

class TileLuaProxy(
    private val name: String,
    private val x: Int,
    private val y: Int,
    private val z: Int
) {
    val Name get() = name
    val Coordinate get() = Coordinate(x, y, z)
    val X get() = x
    val Y get() = y
    val Z get() = z
}