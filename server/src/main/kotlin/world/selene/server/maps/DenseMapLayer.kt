package world.selene.server.maps

import com.google.common.collect.HashBasedTable
import com.google.common.collect.Table
import world.selene.common.util.Coordinate

class DenseMapLayer(override val name: String) : MapLayer, ChunkedMapLayer, BaseMapLayer {

    val chunkSize = 64
    override val chunks = mutableMapOf<Coordinate, DenseChunk>()
    override val visibilityTags = mutableSetOf("default")
    override val collisionTags = mutableSetOf("default")

    override fun getTileId(x: Int, y: Int, z: Int): Int {
        val chunk = getChunkOrNull(x, y, z) ?: return 0
        return chunk.getTileAbsolute(x, y)
    }

    override fun placeTile(
        x: Int,
        y: Int,
        z: Int,
        tileId: Int
    ): Boolean {
        val chunk = getOrCreateChunk(x, y, z)
        if (!chunk.hasTileAbsolute(x, y)) {
            chunk.setTileAbsolute(x, y, tileId)
            return true
        } else {
            return false
        }
    }

    override fun replaceTiles(
        x: Int,
        y: Int,
        z: Int,
        tileId: Int
    ): Boolean {
        val chunk = getOrCreateChunk(x, y, z)
        chunk.setTileAbsolute(x, y, tileId)
        return true
    }

    override fun removeTile(x: Int, y: Int, z: Int, tileId: Int): Boolean {
        val chunk = getOrCreateChunk(x, y, z)
        val currentTileId = chunk.getTileAbsolute(x, y)
        if (currentTileId == tileId) {
            chunk.setTileAbsolute(x, y, 0)
            return true
        }
        return false
    }

    override fun annotateTile(
        x: Int,
        y: Int,
        z: Int,
        key: String,
        data: Map<*, *>
    ) {
        getOrCreateChunk(x, y, z).setAnnotation(Coordinate(x, y, z), key, data)
    }

    override fun resetTile(x: Int, y: Int, z: Int) {
        getChunkOrNull(x, y, z)?.setTileAbsolute(x, y, 0)
    }

    private fun getChunkOrNull(x: Int, y: Int, z: Int): DenseChunk? {
        val startX = Math.floorDiv(x, chunkSize) * chunkSize
        val startY = Math.floorDiv(y, chunkSize) * chunkSize
        return chunks[Coordinate(startX, startY, z)]
    }

    private fun getOrCreateChunk(x: Int, y: Int, z: Int): DenseChunk {
        val startX = Math.floorDiv(x, chunkSize) * chunkSize
        val startY = Math.floorDiv(y, chunkSize) * chunkSize
        val coordinate = Coordinate(startX, startY, z)
        return chunks.getOrPut(coordinate) {
            val chunk = DenseChunk(coordinate, chunkSize)
            chunks[coordinate] = chunk
            return chunk
        }
    }

    inner class DenseChunk(val start: Coordinate, val size: Int) : MapChunk {
        val tiles = IntArray(size * size)
        val annotations = HashBasedTable.create<Coordinate, String, Map<*, *>>()

        fun setAnnotation(coordinate: Coordinate, key: String, value: Map<*, *>) {
            annotations.put(coordinate, key, value)
        }

        fun setTileAbsolute(x: Int, y: Int, tileId: Int) {
            val relativeX = x - start.x
            val relativeY = y - start.y
            val index = relativeX + (relativeY * size)
            if (index < 0 || index >= size * size) {
                throw IllegalArgumentException("Coordinates ($relativeX, $relativeY) is out of bounds")
            }

            tiles[index] = tileId
        }

        fun getTileAbsolute(x: Int, y: Int): Int {
            val relativeX = x - start.x
            val relativeY = y - start.y
            val index = relativeX + (relativeY * size)
            return tiles[index]
        }

        fun hasTileAbsolute(x: Int, y: Int): Boolean {
            val relativeX = x - start.x
            val relativeY = y - start.y
            val index = relativeX + (relativeY * size)
            return index >= 0 && index < size * size && tiles[index] != 0
        }
    }

    override fun addVisibilityTag(tagName: String) {
        visibilityTags.add(tagName)
    }

    override fun removeVisibilityTag(tagName: String) {
        visibilityTags.remove(tagName)
    }

    override fun addCollisionTag(tagName: String) {
        collisionTags.add(tagName)
    }

    override fun removeCollisionTag(tagName: String) {
        collisionTags.remove(tagName)
    }

    fun getAnnotations(): Table<Coordinate, String, Map<*, *>> {
        val result = HashBasedTable.create<Coordinate, String, Map<*, *>>()
        for (chunk in chunks.values) {
            result.putAll(chunk.annotations)
        }
        return result
    }
}