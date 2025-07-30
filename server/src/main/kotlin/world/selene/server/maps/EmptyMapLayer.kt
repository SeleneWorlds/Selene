package world.selene.server.maps

object EmptyMapLayer : MapLayer, BaseMapLayer {
    override val name: String = "empty"
    override val visibilityTags: Set<String> = emptySet()
    override val collisionTags: Set<String> = emptySet()

    override fun placeTile(x: Int, y: Int, z: Int, tileId: Int): Boolean {
        return false
    }

    override fun replaceTiles(x: Int, y: Int, z: Int, tileId: Int): Boolean {
        return false
    }

    override fun removeTile(x: Int, y: Int, z: Int, tileId: Int): Boolean {
        return false
    }

    override fun resetTile(x: Int, y: Int, z: Int) {
    }

    override fun annotateTile(
        x: Int,
        y: Int,
        z: Int,
        key: String,
        data: Map<*, *>
    ) {
    }

    override fun addVisibilityTag(tagName: String) {
    }

    override fun removeVisibilityTag(tagName: String) {
    }

    override fun addCollisionTag(tagName: String) {
    }

    override fun removeCollisionTag(tagName: String) {
    }

    override fun getTileId(x: Int, y: Int, z: Int): Int {
        return 0
    }
}