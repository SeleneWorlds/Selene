package world.selene.server.maps

import world.selene.common.util.Coordinate

class TileLuaProxy(
    private val name: String,
    private val coordinate: Coordinate
) {
    val Name get() = name
    val Coordinate get() = coordinate
    val X get() = coordinate.x
    val Y get() = coordinate.y
    val Z get() = coordinate.z
}