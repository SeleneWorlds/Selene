package world.selene.server.maps

import com.google.common.collect.HashBasedTable
import com.google.common.collect.Table
import world.selene.common.util.Coordinate

class SparseMapLayer(override val name: String) : MapLayer, ChunkedMapLayer {
    private val chunkSize = 64
    override val chunks = mutableMapOf<Coordinate, SparseChunk>()
    override val visibilityTags = mutableSetOf("default")
    override val collisionTags = mutableSetOf("default")

    override fun placeTile(x: Int, y: Int, z: Int, tileId: Int): Boolean {
        val chunk = getOrCreateChunk(x, y, z)
        chunk.addOperation(Coordinate(x, y, z), SparseTilePlacement(x, y, z, tileId))
        return true
    }

    override fun replaceTiles(
        x: Int,
        y: Int,
        z: Int,
        tileId: Int
    ): Boolean {
        val chunk = getOrCreateChunk(x, y, z)
        chunk.addOperation(Coordinate(x, y, z), SparseTilesReplacement(x, y, z, tileId))
        return true
    }

    override fun removeTile(x: Int, y: Int, z: Int, tileId: Int): Boolean {
        val chunk = getOrCreateChunk(x, y, z)
        chunk.addOperation(Coordinate(x, y, z), SparseTileRemoval(x, y, z, tileId))
        return true
    }

    override fun resetTile(x: Int, y: Int, z: Int) {
        getChunkOrNull(x, y, z)?.clearOperations(Coordinate(x, y, z))
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

    fun getOperations(x: Int, y: Int, z: Int): List<SparseOperation> {
        val chunk = getChunkOrNull(x, y, z) ?: return emptyList()
        return chunk.operations[Coordinate(x, y, z)] ?: emptyList()
    }

    private fun getChunkOrNull(x: Int, y: Int, z: Int): SparseChunk? {
        val startX = Math.floorDiv(x, chunkSize) * chunkSize
        val startY = Math.floorDiv(y, chunkSize) * chunkSize
        return chunks[Coordinate(startX, startY, z)]
    }

    private fun getOrCreateChunk(x: Int, y: Int, z: Int): SparseChunk {
        val startX = Math.floorDiv(x, chunkSize) * chunkSize
        val startY = Math.floorDiv(y, chunkSize) * chunkSize
        val coordinate = Coordinate(startX, startY, z)
        return chunks.getOrPut(coordinate) {
            SparseChunk()
        }
    }

    inner class SparseChunk() : MapChunk {
        val operations = mutableMapOf<Coordinate, MutableList<SparseOperation>>()
        val annotations = HashBasedTable.create<Coordinate, String, Map<*, *>>()

        fun addOperation(coordinate: Coordinate, op: SparseOperation) {
            operations.getOrPut(coordinate) { mutableListOf() }.add(op)
        }

        fun clearOperations(coordinate: Coordinate) {
            operations.remove(coordinate)
        }

        fun setAnnotation(coordinate: Coordinate, key: String, data: Map<*, *>) {
            annotations.put(coordinate, key, data)
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

sealed interface SparseOperation

class SparseTilePlacement(val x: Int, val y: Int, val z: Int, val tileId: Int) : SparseOperation

class SparseTileRemoval(val x: Int, val y: Int, val z: Int, val tileId: Int) : SparseOperation

class SparseTilesReplacement(val x: Int, val y: Int, val z: Int, val tileId: Int) :
    SparseOperation