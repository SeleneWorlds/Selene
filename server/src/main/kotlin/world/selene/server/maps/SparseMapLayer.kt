package world.selene.server.maps

import world.selene.common.data.TileDefinition
import world.selene.common.util.Coordinate

class SparseMapLayer(override val name: String) : MapLayer, ChunkedMapLayer {
    private val chunkSize = 64
    override val chunks = mutableMapOf<Coordinate, SparseChunk>()
    override val visibilityTags = mutableSetOf("default")
    override val collisionTags = mutableSetOf("default")

    override fun placeTile(coordinate: Coordinate, tileDef: TileDefinition): Boolean {
        val chunk = getOrCreateChunk(coordinate)
        chunk.addOperation(coordinate, SparseTilePlacement(coordinate, tileDef))
        return true
    }

    override fun replaceTiles(
        coordinate: Coordinate,
        tileDef: TileDefinition
    ): Boolean {
        val chunk = getOrCreateChunk(coordinate)
        chunk.addOperation(coordinate, SparseTilesReplacement(coordinate, tileDef))
        return true
    }

    override fun swapTile(
        coordinate: Coordinate,
        tileDef: TileDefinition,
        newTileDef: TileDefinition
    ): Boolean {
        val chunk = getOrCreateChunk(coordinate)
        chunk.addOperation(coordinate, SparseTileSwap(coordinate, tileDef, newTileDef))
        return true
    }

    override fun removeTile(coordinate: Coordinate, tileDef: TileDefinition): Boolean {
        val chunk = getOrCreateChunk(coordinate)
        chunk.addOperation(coordinate, SparseTileRemoval(coordinate, tileDef))
        return true
    }

    override fun resetTile(coordinate: Coordinate) {
        getChunkOrNull(coordinate)?.clearOperations(coordinate)
    }

    override fun annotateTile(
        coordinate: Coordinate,
        key: String,
        data: Map<Any, Any>?
    ) {
        val chunk = getOrCreateChunk(coordinate)
        chunk.addOperation(coordinate, SparseTileAnnotation(coordinate, key, data))
    }

    fun getOperations(coordinate: Coordinate): List<SparseOperation> {
        val chunk = getChunkOrNull(coordinate) ?: return emptyList()
        return chunk.operations[coordinate] ?: emptyList()
    }

    private fun getChunkOrNull(coordinate: Coordinate): SparseChunk? {
        val startX = Math.floorDiv(coordinate.x, chunkSize) * chunkSize
        val startY = Math.floorDiv(coordinate.y, chunkSize) * chunkSize
        return chunks[Coordinate(startX, startY, coordinate.z)]
    }

    private fun getOrCreateChunk(coordinate: Coordinate): SparseChunk {
        val startX = Math.floorDiv(coordinate.x, chunkSize) * chunkSize
        val startY = Math.floorDiv(coordinate.y, chunkSize) * chunkSize
        val coordinate = Coordinate(startX, startY, coordinate.z)
        return chunks.getOrPut(coordinate) {
            SparseChunk()
        }
    }

    class SparseChunk() : MapChunk {
        val operations = mutableMapOf<Coordinate, MutableList<SparseOperation>>()

        fun addOperation(coordinate: Coordinate, op: SparseOperation) {
            operations.getOrPut(coordinate) { mutableListOf() }.add(op)
        }

        fun clearOperations(coordinate: Coordinate) {
            operations.remove(coordinate)
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
}

sealed interface SparseOperation

class SparseTilePlacement(val coordinate: Coordinate, val tileDef: TileDefinition) : SparseOperation

class SparseTileRemoval(val coordinate: Coordinate, val tileDef: TileDefinition) : SparseOperation

class SparseTilesReplacement(val coordinate: Coordinate, val tileDef: TileDefinition) :
    SparseOperation

class SparseTileSwap(val coordinate: Coordinate, val oldTileDef: TileDefinition, val newTileDef: TileDefinition) :
    SparseOperation

class SparseTileAnnotation(val coordinate: Coordinate, val key: String, val data: Map<Any, Any>?) :
    SparseOperation