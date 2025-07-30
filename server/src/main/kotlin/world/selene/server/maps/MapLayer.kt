package world.selene.server.maps

interface MapLayer {
    val name: String
    val visibilityTags: Set<String>
    val collisionTags: Set<String>
    fun placeTile(x: Int, y: Int, z: Int, tileId: Int): Boolean
    fun replaceTiles(x: Int, y: Int, z: Int, tileId: Int): Boolean
    fun removeTile(x: Int, y: Int, z: Int, tileId: Int): Boolean
    fun resetTile(x: Int, y: Int, z: Int)
    fun annotateTile(x: Int, y: Int, z: Int, key: String, data: Map<*, *>)
    fun addVisibilityTag(tagName: String)
    fun removeVisibilityTag(tagName: String)
    fun addCollisionTag(tagName: String)
    fun removeCollisionTag(tagName: String)
}