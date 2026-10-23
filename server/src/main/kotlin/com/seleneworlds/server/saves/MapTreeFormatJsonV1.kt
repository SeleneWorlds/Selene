package com.seleneworlds.server.saves

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import com.seleneworlds.common.grid.Coordinate
import com.seleneworlds.common.serialization.NullableSerializedMapSerializer
import com.seleneworlds.common.serialization.SerializedMap
import com.seleneworlds.common.serialization.SerializedMapSerializer
import com.seleneworlds.common.serialization.decodeFromFile
import com.seleneworlds.server.data.Registries
import com.seleneworlds.server.maps.layers.MapLayerApi
import com.seleneworlds.server.maps.layers.MapLayer
import com.seleneworlds.server.maps.layers.ChunkedMapLayer
import com.seleneworlds.server.maps.layers.DenseMapLayer
import com.seleneworlds.server.maps.layers.SparseMapLayer
import com.seleneworlds.server.maps.layers.SparseTileAnnotation
import com.seleneworlds.server.maps.layers.SparseTilePlacement
import com.seleneworlds.server.maps.layers.SparseTileRemoval
import com.seleneworlds.server.maps.layers.SparseTileSwap
import com.seleneworlds.server.maps.layers.SparseTilesReplacement
import com.seleneworlds.server.maps.tree.MapTree
import java.io.File

class MapTreeFormatJsonV1(private val registries: Registries, private val json: Json) : MapTreeFormat {

    @Serializable
    data class SaveHeader(val version: Int)
    @Serializable
    data class LayerData(
        val chunks: List<MapTreeFileChunk>,
        val name: String? = null,
        val kind: String? = null
    )

    @Serializable
    sealed interface MapTreeFileChunk
    @Serializable
    data class MapTreeFileAnnotation(
        val x: Int,
        val y: Int,
        val z: Int,
        val key: String,
        @Serializable(with = SerializedMapSerializer::class)
        val data: SerializedMap
    )
    @Serializable
    @SerialName("dense")
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

    @Serializable
    @SerialName("sparse")
    data class MapTreeFileSparseChunk(
        val x: Int,
        val y: Int,
        val z: Int,
        val operations: List<MapTreeFileSparseOperation>
    ) : MapTreeFileChunk

    @Serializable
    @SerialName("unknown")
    object MapTreeFileUnknownChunk : MapTreeFileChunk

    @Serializable
    sealed interface MapTreeFileSparseOperation
    @Serializable
    @SerialName("add")
    data class MapTreeFileSparseAddOperation(val x: Int, val y: Int, val z: Int, val tileId: Int) :
        MapTreeFileSparseOperation

    @Serializable
    @SerialName("remove")
    data class MapTreeFileSparseRemoveOperation(val x: Int, val y: Int, val z: Int, val tileId: Int) :
        MapTreeFileSparseOperation

    @Serializable
    @SerialName("replace")
    data class MapTreeFileSparseReplaceOperation(
        val x: Int,
        val y: Int,
        val z: Int,
        val tileId: Int
    ) : MapTreeFileSparseOperation

    @Serializable
    @SerialName("swap")
    data class MapTreeFileSparseSwapOperation(
        val x: Int,
        val y: Int,
        val z: Int,
        val oldTileId: Int,
        val newTileId: Int
    ) : MapTreeFileSparseOperation

    @Serializable
    @SerialName("annotation")
    data class MapTreeFileSparseAnnotationOperation(
        val x: Int,
        val y: Int,
        val z: Int,
        val key: String,
        @Serializable(with = NullableSerializedMapSerializer::class)
        val data: SerializedMap?
    ) : MapTreeFileSparseOperation

    @Serializable
    data class MapTreeFile(val header: SaveHeader, val layers: List<LayerData>)

    @Serializable
    data class MapLayerFile(val header: SaveHeader, val layer: LayerData)

    override fun loadMapTree(file: File): MapTree {
        val saved = json.decodeFromFile(MapTreeFile.serializer(), file)
        require(saved.header.version in 1..VERSION) { "Unsupported map version: ${saved.header.version}" }
        return MapTree(registries).also { tree ->
            saved.layers.forEachIndexed { index, layer ->
                tree.addLayer(decodeLayer(layer, "loaded-$index"))
            }
        }
    }

    override fun loadMapLayer(file: File): MapLayerApi {
        val saved = json.decodeFromFile(MapLayerFile.serializer(), file)
        require(saved.header.version == VERSION) { "Unsupported layer version: ${saved.header.version}" }
        val name = requireNotNull(saved.layer.name) { "A layer save must have a name" }
        return MapLayerApi(decodeLayer(saved.layer, name), registries)
    }

    private fun decodeLayer(savedLayer: LayerData, fallbackName: String): MapLayer {
        val name = savedLayer.name ?: fallbackName
        val kind = savedLayer.kind ?: if (savedLayer.chunks.any { it is MapTreeFileDenseChunk }) "dense" else "sparse"
        val layer = when (kind) {
            "dense" -> DenseMapLayer(name, registries)
            "sparse" -> SparseMapLayer(name)
            else -> error("Unknown map layer kind: $kind")
        }
        for (chunk in savedLayer.chunks) {
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
                                    layer.placeTile(Coordinate(baseX + dx, baseY + dy, z), tileDef)
                                }
                            }
                        }
                    }
                    chunk.annotations.forEach { annotation ->
                        layer.annotateTile(
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
                                layer.placeTile(Coordinate(op.x, op.y, op.z), tileDef)
                            }

                            is MapTreeFileSparseSwapOperation -> {
                                val oldTileDef = registries.tiles.get(op.oldTileId)
                                    ?: throw RuntimeException("Missing tile definition for id ${op.oldTileId}")
                                val newTileDef = registries.tiles.get(op.newTileId)
                                    ?: throw RuntimeException("Missing tile definition for id ${op.newTileId}")
                                layer.swapTile(Coordinate(op.x, op.y, op.z), oldTileDef, newTileDef)
                            }

                            is MapTreeFileSparseAnnotationOperation -> {
                                layer.annotateTile(Coordinate(op.x, op.y, op.z), op.key, op.data)
                            }

                            is MapTreeFileSparseReplaceOperation -> {
                                val tileDef = registries.tiles.get(op.tileId)
                                    ?: throw RuntimeException("Missing tile definition for id ${op.tileId}")
                                layer.replaceTiles(Coordinate(op.x, op.y, op.z), tileDef)
                            }

                            is MapTreeFileSparseRemoveOperation -> {
                                val tileDef = registries.tiles.get(op.tileId)
                                    ?: throw RuntimeException("Missing tile definition for id ${op.tileId}")
                                layer.removeTile(Coordinate(op.x, op.y, op.z), tileDef)
                            }
                        }
                    }
                }

                is MapTreeFileUnknownChunk -> Unit
            }
        }
        return layer
    }

    override fun saveFullyInline(file: File, mapTree: MapTree) {
        val saved = MapTreeFile(SaveHeader(VERSION), mapTree.layers.map(::encodeLayer))
        file.writeText(json.encodeToString(MapTreeFile.serializer(), saved))
    }

    override fun saveLayer(file: File, layer: MapLayerApi) {
        require(layer.layer is DenseMapLayer || layer.layer is SparseMapLayer) { "Unsupported standalone layer" }
        val saved = MapLayerFile(SaveHeader(VERSION), encodeLayer(layer.layer))
        file.writeText(json.encodeToString(MapLayerFile.serializer(), saved))
    }

    private fun encodeLayer(layer: MapLayer): LayerData {
        return if (layer is ChunkedMapLayer) {
            LayerData(
                chunks = layer.chunks.map { (chunkCoordinate, chunk) ->
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
                },
                name = layer.name,
                kind = when (layer) {
                    is DenseMapLayer -> "dense"
                    is SparseMapLayer -> "sparse"
                    else -> error("Unsupported map layer")
                }
            )
        } else {
            LayerData(emptyList())
        }
    }

    companion object {
        const val VERSION = 2
    }
}
