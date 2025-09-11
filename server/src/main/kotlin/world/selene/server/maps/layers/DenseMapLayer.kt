package world.selene.server.maps.layers

import com.google.common.collect.HashBasedTable
import com.google.common.collect.Table
import world.selene.common.tiles.TileDefinition
import world.selene.common.grid.Coordinate
import world.selene.server.data.Registries
import world.selene.server.maps.MapChunk

class DenseMapLayer(override val name: String, private val registries: Registries) : MapLayer, ChunkedMapLayer,
    BaseMapLayer {

    override val chunks = mutableMapOf<Coordinate, DenseChunk>()
    override val visibilityTags = mutableSetOf("default")
    override val collisionTags = mutableSetOf("default")

    override fun getTileId(coordinate: Coordinate): Int {
        val chunk = getChunkOrNull(coordinate) ?: return 0
        return chunk.getTileIdAbsolute(coordinate.x, coordinate.y)
    }

    override fun placeTile(
        coordinate: Coordinate,
        tileDef: TileDefinition
    ): Boolean {
        val chunk = getOrCreateChunk(coordinate)
        if (!chunk.hasTileAbsolute(coordinate.x, coordinate.y)) {
            chunk.setTileAbsolute(coordinate.x, coordinate.y, tileDef)
            return true
        } else {
            return false
        }
    }

    override fun swapTile(
        coordinate: Coordinate,
        tileDef: TileDefinition,
        newTileDef: TileDefinition
    ): Boolean {
        val chunk = getOrCreateChunk(coordinate)
        val currentTileDef = chunk.getTileAbsolute(coordinate.x, coordinate.y)
        if (currentTileDef == tileDef) {
            chunk.setTileAbsolute(coordinate.x, coordinate.y, newTileDef)
            return true
        }
        return false
    }

    override fun replaceTiles(
        coordinate: Coordinate,
        tileDef: TileDefinition
    ): Boolean {
        val chunk = getOrCreateChunk(coordinate)
        chunk.setTileAbsolute(coordinate.x, coordinate.y, tileDef)
        return true
    }

    override fun removeTile(coordinate: Coordinate, tileDef: TileDefinition): Boolean {
        val chunk = getOrCreateChunk(coordinate)
        val currentTileDef = chunk.getTileAbsolute(coordinate.x, coordinate.y)
        if (currentTileDef == tileDef) {
            chunk.setTileAbsolute(coordinate.x, coordinate.y, null)
            return true
        }
        return false
    }

    override fun annotateTile(
        coordinate: Coordinate,
        key: String,
        data: Map<Any, Any>?
    ) {
        getOrCreateChunk(coordinate).setAnnotation(coordinate, key, data)
    }

    override fun resetTile(coordinate: Coordinate) {
        getChunkOrNull(coordinate)?.setTileAbsolute(coordinate.x, coordinate.y, null)
    }

    private fun getChunkOrNull(coordinate: Coordinate): DenseChunk? {
        val startX = Math.floorDiv(coordinate.x, CHUNK_SIZE) * CHUNK_SIZE
        val startY = Math.floorDiv(coordinate.y, CHUNK_SIZE) * CHUNK_SIZE
        return chunks[Coordinate(startX, startY, coordinate.z)]
    }

    private fun getOrCreateChunk(coordinate: Coordinate): DenseChunk {
        val startX = Math.floorDiv(coordinate.x, CHUNK_SIZE) * CHUNK_SIZE
        val startY = Math.floorDiv(coordinate.y, CHUNK_SIZE) * CHUNK_SIZE
        val coordinate = Coordinate(startX, startY, coordinate.z)
        return chunks.getOrPut(coordinate) {
            val chunk = DenseChunk(coordinate, CHUNK_SIZE)
            chunks[coordinate] = chunk
            return chunk
        }
    }

    inner class DenseChunk(val start: Coordinate, val size: Int) : MapChunk {
        val tiles = IntArray(size * size)
        val annotations = HashBasedTable.create<Coordinate, String, Map<Any, Any>>()

        fun setAnnotation(coordinate: Coordinate, key: String, value: Map<Any, Any>?) {
            if (value != null) {
                annotations.put(coordinate, key, value)
            } else {
                annotations.remove(coordinate, key)
            }
        }

        fun setTileAbsolute(x: Int, y: Int, tileDef: TileDefinition?) {
            val relativeX = x - start.x
            val relativeY = y - start.y
            val index = relativeX + (relativeY * size)
            if (index < 0 || index >= size * size) {
                throw IllegalArgumentException("Coordinates ($relativeX, $relativeY) is out of bounds")
            }

            val tileId = tileDef?.id ?: 0
            tiles[index] = tileId
        }

        fun getTileAbsolute(x: Int, y: Int): TileDefinition {
            val tileId = getTileIdAbsolute(x, y)
            return registries.tiles.get(tileId) ?: throw IllegalArgumentException("Tile $tileId has no definition")
        }

        fun getTileIdAbsolute(x: Int, y: Int): Int {
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

    companion object {
        const val CHUNK_SIZE = 64
    }
}