package world.selene.server.saves

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.databind.ObjectMapper
import world.selene.common.util.Coordinate
import world.selene.server.data.Registries
import world.selene.server.maps.ChunkedMapLayer
import world.selene.server.maps.DenseMapLayer
import world.selene.server.maps.MapTree
import world.selene.server.maps.SparseMapLayer
import world.selene.server.maps.SparseTileAnnotation
import world.selene.server.maps.SparseTilePlacement
import world.selene.server.maps.SparseTileRemoval
import world.selene.server.maps.SparseTileSwap
import world.selene.server.maps.SparseTilesReplacement
import java.io.File

class MapTreeFormatJsonV1(private val registries: Registries, private val objectMapper: ObjectMapper) : MapTreeFormat {

    data class MapTreeFileHeader(val version: Int)
    data class MapTreeFileLayer(val chunks: List<MapTreeFileChunk>)

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
    @JsonSubTypes(
        JsonSubTypes.Type(value = MapTreeFileDenseChunk::class, name = "dense"),
        JsonSubTypes.Type(value = MapTreeFileSparseChunk::class, name = "sparse"),
        JsonSubTypes.Type(value = MapTreeFileUnknownChunk::class, name = "unknown")
    )
    interface MapTreeFileChunk
    data class MapTreeFileAnnotation(val x: Int, val y: Int, val z: Int, val key: String, val data: Map<Any, Any>)
    data class MapTreeFileDenseChunk(
        val x: Int,
        val y: Int,
        val z: Int,
        val tiles: IntArray,
        val annotations: List<MapTreeFileAnnotation>
    ) : MapTreeFileChunk {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as MapTreeFileDenseChunk

            if (x != other.x) return false
            if (y != other.y) return false
            if (z != other.z) return false
            if (!tiles.contentEquals(other.tiles)) return false
            if (annotations != other.annotations) return false

            return true
        }

        override fun hashCode(): Int {
            var result = x
            result = 31 * result + y
            result = 31 * result + z
            result = 31 * result + tiles.contentHashCode()
            result = 31 * result + annotations.hashCode()
            return result
        }
    }

    data class MapTreeFileSparseChunk(
        val x: Int,
        val y: Int,
        val z: Int,
        val operations: List<MapTreeFileSparseOperation>
    ) : MapTreeFileChunk

    object MapTreeFileUnknownChunk : MapTreeFileChunk

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
    @JsonSubTypes(
        JsonSubTypes.Type(value = MapTreeFileSparseAddOperation::class, name = "add"),
        JsonSubTypes.Type(value = MapTreeFileSparseRemoveOperation::class, name = "remove"),
        JsonSubTypes.Type(value = MapTreeFileSparseReplaceOperation::class, name = "replace"),
        JsonSubTypes.Type(value = MapTreeFileSparseSwapOperation::class, name = "swap")
    )
    sealed interface MapTreeFileSparseOperation
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

    data class MapTreeFileSparseSwapOperation(
        val x: Int,
        val y: Int,
        val z: Int,
        val oldTileId: Int,
        val newTileId: Int
    ) : MapTreeFileSparseOperation

    data class MapTreeFileSparseAnnotationOperation(
        val x: Int,
        val y: Int,
        val z: Int,
        val key: String,
        val data: Map<Any, Any>?
    ) : MapTreeFileSparseOperation

    data class MapTreeFile(val header: MapTreeFileHeader, val layers: List<MapTreeFileLayer>)

    override fun load(file: File): MapTree {
        val result = MapTree(registries)
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
                                        val tileDef = registries.tiles.get(tileId)
                                            ?: throw RuntimeException("Missing tile definition for id $tileId")
                                        result.placeTile(Coordinate(baseX + dx, baseY + dy, z), tileDef)
                                    }
                                }
                            }
                        }
                        chunk.annotations.forEach { annotation ->
                            result.annotateTile(
                                Coordinate(annotation.x, annotation.y, annotation.z),
                                annotation.key,
                                annotation.data
                            )
                        }
                    }

                    is MapTreeFileSparseChunk -> {
                        for (op in chunk.operations) {
                            when (op) {
                                is MapTreeFileSparseAddOperation -> {
                                    val tileDef = registries.tiles.get(op.tileId)
                                        ?: throw RuntimeException("Missing tile definition for id ${op.tileId}")
                                    result.placeTile(Coordinate(op.x, op.y, op.z), tileDef)
                                }

                                is MapTreeFileSparseSwapOperation -> {
                                    val oldTileDef = registries.tiles.get(op.oldTileId)
                                        ?: throw RuntimeException("Missing tile definition for id ${op.oldTileId}")
                                    val newTileDef = registries.tiles.get(op.newTileId)
                                        ?: throw RuntimeException("Missing tile definition for id ${op.newTileId}")
                                    result.swapTile(Coordinate(op.x, op.y, op.z), oldTileDef, newTileDef)
                                }

                                is MapTreeFileSparseAnnotationOperation -> {
                                    result.annotateTile(Coordinate(op.x, op.y, op.z), op.key, op.data)
                                }

                                is MapTreeFileSparseReplaceOperation -> {
                                    val tileDef = registries.tiles.get(op.tileId)
                                        ?: throw RuntimeException("Missing tile definition for id ${op.tileId}")
                                    result.replaceTiles(Coordinate(op.x, op.y, op.z), tileDef)
                                }

                                is MapTreeFileSparseRemoveOperation -> {
                                    val tileDef = registries.tiles.get(op.tileId)
                                        ?: throw RuntimeException("Missing tile definition for id ${op.tileId}")
                                    result.removeTile(Coordinate(op.x, op.y, op.z), tileDef)
                                }
                            }
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
                                                        operation.tileDef.id
                                                    )

                                                    is SparseTileSwap -> MapTreeFileSparseSwapOperation(
                                                        coordinate.x,
                                                        coordinate.y,
                                                        coordinate.z,
                                                        operation.oldTileDef.id,
                                                        operation.newTileDef.id
                                                    )

                                                    is SparseTileAnnotation -> MapTreeFileSparseAnnotationOperation(
                                                        coordinate.x,
                                                        coordinate.y,
                                                        coordinate.z,
                                                        operation.key,
                                                        operation.data
                                                    )

                                                    is SparseTileRemoval -> MapTreeFileSparseRemoveOperation(
                                                        coordinate.x,
                                                        coordinate.y,
                                                        coordinate.z,
                                                        operation.tileDef.id
                                                    )

                                                    is SparseTilesReplacement -> MapTreeFileSparseReplaceOperation(
                                                        coordinate.x,
                                                        coordinate.y,
                                                        coordinate.z,
                                                        operation.tileDef.id
                                                    )
                                                }
                                            }
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