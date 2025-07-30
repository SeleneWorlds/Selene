package world.selene.server.saves

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.databind.ObjectMapper
import world.selene.server.maps.ChunkedMapLayer
import world.selene.server.maps.DenseMapLayer
import world.selene.server.maps.MapManager
import world.selene.server.maps.MapTree
import world.selene.server.maps.SparseMapLayer
import world.selene.server.maps.SparseTilePlacement
import world.selene.server.maps.SparseTileRemoval
import world.selene.server.maps.SparseTilesReplacement
import java.io.File

class MapTreeFormatJsonV1(private val mapManager: MapManager, private val objectMapper: ObjectMapper) : MapTreeFormat {

    data class MapTreeFileHeader(val version: Int)
    data class MapTreeFileLayer(val chunks: List<MapTreeFileChunk>)

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
    @JsonSubTypes(
        JsonSubTypes.Type(value = MapTreeFileDenseChunk::class, name = "dense"),
        JsonSubTypes.Type(value = MapTreeFileSparseChunk::class, name = "sparse"),
        JsonSubTypes.Type(value = MapTreeFileUnknownChunk::class, name = "unknown")
    )
    interface MapTreeFileChunk
    data class MapTreeFileAnnotation(val x: Int, val y: Int, val z: Int, val key: String, val data: Map<*, *>)
    data class MapTreeFileDenseChunk(
        val x: Int,
        val y: Int,
        val z: Int,
        val tiles: IntArray,
        val annotations: List<MapTreeFileAnnotation>
    ) : MapTreeFileChunk

    data class MapTreeFileSparseChunk(
        val x: Int,
        val y: Int,
        val z: Int,
        val operations: List<MapTreeFileSparseOperation>,
        val annotations: List<MapTreeFileAnnotation>
    ) : MapTreeFileChunk

    object MapTreeFileUnknownChunk : MapTreeFileChunk

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
    @JsonSubTypes(
        JsonSubTypes.Type(value = MapTreeFileSparseAddOperation::class, name = "add"),
        JsonSubTypes.Type(value = MapTreeFileSparseRemoveOperation::class, name = "remove"),
        JsonSubTypes.Type(value = MapTreeFileSparseReplaceOperation::class, name = "replace")
    )
    interface MapTreeFileSparseOperation
    data class MapTreeFileSparseAddOperation(val x: Int, val y: Int, val z: Int, val tileId: Int) :
        MapTreeFileSparseOperation

    data class MapTreeFileSparseRemoveOperation(val x: Int, val y: Int, val z: Int, val tileId: Int) :
        MapTreeFileSparseOperation

    data class MapTreeFileSparseReplaceOperation(
        val x: Int,
        val y: Int,
        val z: Int,
        val tileId: Int
    ) : MapTreeFileSparseOperation

    data class MapTreeFile(val header: MapTreeFileHeader, val layers: List<MapTreeFileLayer>)

    override fun load(file: File): MapTree {
        val result = mapManager.createMapTree()
        val mapTreeFile = objectMapper.readValue(file, MapTreeFile::class.java)
        for (layer in mapTreeFile.layers) {
            for (chunk in layer.chunks) {
                when (chunk) {
                    is MapTreeFileDenseChunk -> {
                        // Dense chunk: tiles is a flat IntArray
                        // Assume chunk size is 64x64 (matching DenseMapLayer default)
                        val chunkSize = 64
                        val baseX = chunk.x
                        val baseY = chunk.y
                        val z = chunk.z
                        val tiles = chunk.tiles
                        for (dy in 0 until chunkSize) {
                            for (dx in 0 until chunkSize) {
                                val index = dx + dy * chunkSize
                                if (index < tiles.size) {
                                    val tileId = tiles[index]
                                    if (tileId != 0) {
                                        result.placeTile(baseX + dx, baseY + dy, z, tileId)
                                    }
                                }
                            }
                        }
                        chunk.annotations.forEach { annotation ->
                            result.annotateTile(
                                annotation.x,
                                annotation.y,
                                annotation.z,
                                annotation.key,
                                annotation.data
                            )
                        }
                    }

                    is MapTreeFileSparseChunk -> {
                        for (op in chunk.operations) {
                            when (op) {
                                is MapTreeFileSparseAddOperation -> {
                                    result.placeTile(op.x, op.y, op.z, op.tileId)
                                }

                                is MapTreeFileSparseReplaceOperation -> {
                                    result.replaceTiles(op.x, op.y, op.z, op.tileId)
                                }

                                is MapTreeFileSparseRemoveOperation -> {
                                    result.removeTile(op.x, op.y, op.z, op.tileId)
                                }
                            }
                        }
                        chunk.annotations.forEach { annotation ->
                            result.annotateTile(
                                annotation.x,
                                annotation.y,
                                annotation.z,
                                annotation.key,
                                annotation.data
                            )
                        }
                    }

                    is MapTreeFileUnknownChunk -> Unit
                }
            }
        }
        return result
    }

    override fun saveFullyInline(file: File, mapTree: MapTree) {
        val output = MapTreeFile(
            header = MapTreeFileHeader(VERSION),
            layers = mapTree.layers.map { layer ->
                if (layer is ChunkedMapLayer) {
                    MapTreeFileLayer(
                        layer.chunks.map { (chunkCoordinate, chunk) ->
                            when (chunk) {
                                is DenseMapLayer.DenseChunk -> {
                                    MapTreeFileDenseChunk(
                                        chunkCoordinate.x,
                                        chunkCoordinate.y,
                                        chunkCoordinate.z,
                                        chunk.tiles,
                                        chunk.annotations.cellSet().map { cell ->
                                            MapTreeFileAnnotation(
                                                cell.rowKey.x,
                                                cell.rowKey.y,
                                                cell.rowKey.z,
                                                cell.columnKey,
                                                cell.value
                                            )
                                        }
                                    )
                                }

                                is SparseMapLayer.SparseChunk -> {
                                    MapTreeFileSparseChunk(
                                        chunkCoordinate.x,
                                        chunkCoordinate.y,
                                        chunkCoordinate.z,
                                        chunk.operations.flatMap { (coordinate, operations) ->
                                            operations.map { operation ->
                                                when (operation) {
                                                    is SparseTilePlacement -> MapTreeFileSparseAddOperation(
                                                        coordinate.x,
                                                        coordinate.y,
                                                        coordinate.z,
                                                        operation.tileId
                                                    )

                                                    is SparseTileRemoval -> MapTreeFileSparseRemoveOperation(
                                                        coordinate.x,
                                                        coordinate.y,
                                                        coordinate.z,
                                                        operation.tileId
                                                    )

                                                    is SparseTilesReplacement -> MapTreeFileSparseReplaceOperation(
                                                        coordinate.x,
                                                        coordinate.y,
                                                        coordinate.z,
                                                        operation.tileId
                                                    )
                                                }
                                            }
                                        },
                                        chunk.annotations.cellSet().map { cell ->
                                            MapTreeFileAnnotation(
                                                cell.rowKey.x,
                                                cell.rowKey.y,
                                                cell.rowKey.z,
                                                cell.columnKey,
                                                cell.value
                                            )
                                        }
                                    )
                                }

                                else -> {
                                    MapTreeFileUnknownChunk
                                }
                            }
                        }
                    )
                } else {
                    MapTreeFileLayer(emptyList())
                }
            }
        )
        objectMapper.writeValue(file, output)
    }

    companion object {
        const val VERSION = 1
    }
}