package world.selene.server.maps

interface BaseMapLayer : MapLayer {
    fun getTileId(x: Int, y: Int, z: Int): Int
}