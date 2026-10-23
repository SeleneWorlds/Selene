package com.seleneworlds.server.saves

import com.seleneworlds.common.grid.Coordinate
import com.seleneworlds.common.serialization.SerializedMap
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
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.charset.StandardCharsets

class MapTreeFormatBinaryV1(private val registries: Registries) : MapTreeFormat {

    override fun loadMapTree(file: File): MapTree {
        val buffer = ByteBuffer.wrap(file.readBytes()).order(ByteOrder.BIG_ENDIAN)
        buffer.readHeader(MAGIC, VERSION)
        val headers = List(buffer.short.toInt()) { readLayerHeader(buffer) }
        return MapTree(registries).also { tree ->
            headers.forEachIndexed { index, header ->
                tree.addLayer(readLayer(buffer, header, "loaded-$index"))
            }
        }
    }

    override fun loadMapLayer(file: File): MapLayerApi {
        val buffer = ByteBuffer.wrap(file.readBytes()).order(ByteOrder.BIG_ENDIAN)
        buffer.readHeader(LAYER_MAGIC, LAYER_VERSION)
        val name = buffer.readString()
        val header = readLayerHeader(buffer)
        return MapLayerApi(readLayer(buffer, header, name), registries)
    }

    private fun ByteBuffer.readHeader(expectedMagic: String, expectedVersion: Int) {
        val magic = ByteArray(4).also { get(it) }
        require(String(magic, StandardCharsets.UTF_8) == expectedMagic) { "Expected $expectedMagic save" }
        require(short.toInt() == expectedVersion) { "Unsupported save version" }
        require(int == 0) { "Unsupported save flags" }
    }

    private fun readLayerHeader(buffer: ByteBuffer): LayerHeader {
        val layerType = buffer.get().toInt()
        val chunkCount = buffer.int
        val chunks = mutableListOf<ChunkHeader>()

        repeat(chunkCount) {
            val x = buffer.short.toInt()
            val y = buffer.short.toInt()
            val z = buffer.short.toInt()
            val storageType = buffer.get().toInt()
            val offset = buffer.int
            val length = buffer.int

            chunks.add(ChunkHeader(Coordinate(x, y, z), storageType, offset, length))
        }

        return LayerHeader(layerType, chunks)
    }

    private fun readLayer(buffer: ByteBuffer, layerHeader: LayerHeader, name: String): MapLayer {
        val layer = when (layerHeader.type) {
            LAYER_SPARSE -> SparseMapLayer(name)
            LAYER_DENSE -> DenseMapLayer(name, registries)
            else -> error("Unsupported map layer type: ${layerHeader.type}")
        }
        layerHeader.chunks.forEach { chunkHeader ->
            buffer.position(chunkHeader.offset)

            val chunkType = buffer.get().toInt()

            when (chunkType) {
                CHUNK_DENSE -> {
                    // Read dense chunk data - bulk read all tiles at once
                    val chunkSize = DenseMapLayer.CHUNK_SIZE
                    val baseX = chunkHeader.coordinate.x
                    val baseY = chunkHeader.coordinate.y
                    val z = chunkHeader.coordinate.z

                    val tileCount = chunkSize * chunkSize
                    val tileData = IntArray(tileCount)
                    for (i in 0 until tileCount) {
                        tileData[i] = buffer.int
                    }

                    // Process tiles
                    for (dy in 0 until chunkSize) {
                        for (dx in 0 until chunkSize) {
                            val index = dx + dy * chunkSize
                            val tileId = tileData[index]
                            if (tileId != 0) {
                                val tileDef = registries.tiles.get(tileId)
                                    ?: throw RuntimeException("Missing tile definition for id $tileId")
                                layer.placeTile(Coordinate(baseX + dx, baseY + dy, z), tileDef)
                            }
                        }
                    }

                    // Read annotations
                    val annotationCount = buffer.int
                    repeat(annotationCount) {
                        val x = buffer.int
                        val y = buffer.int
                        val z = buffer.int
                        val key = buffer.readString()
                        val data = buffer.readMap()
                        layer.annotateTile(Coordinate(x, y, z), key, data)
                    }
                }

                CHUNK_SPARSE -> {
                    val operationCoordinateCount = buffer.int
                    repeat(operationCoordinateCount) {
                        val operationCount = buffer.int
                        repeat(operationCount) {
                            val x = buffer.short.toInt()
                            val y = buffer.short.toInt()
                            val z = buffer.short.toInt()
                            val operationType = buffer.get().toInt()

                            when (operationType) {
                                OP_PLACEMENT -> {
                                    val tileId = buffer.int
                                    val tileDef = registries.tiles.get(tileId)
                                        ?: throw RuntimeException("Missing tile definition for id $tileId")
                                    layer.placeTile(Coordinate(x, y, z), tileDef)
                                }

                                OP_REMOVAL -> {
                                    val tileId = buffer.int
                                    val tileDef = registries.tiles.get(tileId)
                                        ?: throw RuntimeException("Missing tile definition for id $tileId")
                                    layer.removeTile(Coordinate(x, y, z), tileDef)
                                }

                                OP_REPLACEMENT -> {
                                    val tileId = buffer.int
                                    val tileDef = registries.tiles.get(tileId)
                                        ?: throw RuntimeException("Missing tile definition for id $tileId")
                                    layer.replaceTiles(Coordinate(x, y, z), tileDef)
                                }

                                OP_SWAP -> {
                                    val oldTileId = buffer.int
                                    val newTileId = buffer.int
                                    val oldTileDef = registries.tiles.get(oldTileId)
                                        ?: throw RuntimeException("Missing tile definition for id $oldTileId")
                                    val newTileDef = registries.tiles.get(newTileId)
                                        ?: throw RuntimeException("Missing tile definition for id $newTileId")
                                    layer.swapTile(Coordinate(x, y, z), oldTileDef, newTileDef)
                                }

                                OP_ANNOTATION -> {
                                    val annotationX = buffer.int
                                    val annotationY = buffer.int
                                    val annotationZ = buffer.int
                                    val key = buffer.readString()
                                    val hasData = buffer.get().toInt() == 1
                                    val data = if (hasData) buffer.readMap() else null
                                    layer.annotateTile(Coordinate(annotationX, annotationY, annotationZ), key, data)
                                }
                            }
                        }
                    }
                }
            }
        }
        return layer
    }

    override fun saveFullyInline(file: File, mapTree: MapTree) {
        RandomAccessFile(file, "rw").use { raf ->
            raf.setLength(0)
            raf.writeHeader(MAGIC, VERSION)
            raf.writeShort(mapTree.layers.size)
            val headers = mapTree.layers.map { writeLayerHeader(raf, it) }
            mapTree.layers.forEachIndexed { index, layer -> writeLayerChunks(raf, layer, headers[index]) }
        }
    }

    override fun saveLayer(file: File, layer: MapLayerApi) {
        require(layer.layer is DenseMapLayer || layer.layer is SparseMapLayer) { "Unsupported standalone layer" }
        RandomAccessFile(file, "rw").use { raf ->
            raf.setLength(0)
            raf.writeHeader(LAYER_MAGIC, LAYER_VERSION)
            raf.writeString(layer.getName())
            val header = writeLayerHeader(raf, layer.layer)
            writeLayerChunks(raf, layer.layer, header)
        }
    }

    private fun RandomAccessFile.writeHeader(magic: String, version: Int) {
        writeBytes(magic)
        writeShort(version)
        writeInt(0) // reserved
    }

    private fun writeLayerHeader(raf: RandomAccessFile, layer: MapLayer): List<Long> {
        val chunkHeaderPointers = mutableListOf<Long>()
        raf.writeByte(
            when (layer) {
                is DenseMapLayer -> LAYER_DENSE
                is SparseMapLayer -> LAYER_SPARSE
                else -> 0
            }
        )
        if (layer is ChunkedMapLayer) {
            raf.writeInt(layer.chunks.size)
            layer.chunks.forEach { (chunkStart, _) ->
                raf.writeShort(chunkStart.x)
                raf.writeShort(chunkStart.y)
                raf.writeShort(chunkStart.z)
                raf.writeByte(STORAGE_INLINE)
                chunkHeaderPointers += raf.filePointer
                raf.writeInt(0) // Chunk Offset Placeholder
                raf.writeInt(0) // Chunk Length Placeholder
            }
        } else {
            raf.writeInt(1)
            raf.writeShort(0)
            raf.writeShort(0)
            raf.writeShort(0)
            raf.writeByte(STORAGE_INLINE)
            chunkHeaderPointers += raf.filePointer
            raf.writeInt(0) // Chunk Offset Placeholder
            raf.writeInt(0) // Chunk Length Placeholder
        }
        return chunkHeaderPointers
    }

    private fun writeLayerChunks(raf: RandomAccessFile, layer: MapLayer, chunkHeaderPointers: List<Long>) {
        var chunkIndex = 0
        if (layer is ChunkedMapLayer) {
            layer.chunks.forEach { (_, chunk) ->
                val chunkPointer = raf.filePointer
                raf.writeByte(
                    when (chunk) {
                        is DenseMapLayer.DenseChunk -> CHUNK_DENSE
                        is SparseMapLayer.SparseChunk -> CHUNK_SPARSE
                        else -> 0
                    }
                )

                when (chunk) {
                    is DenseMapLayer.DenseChunk -> {
                        // Write dense tile data - bulk write for performance
                        val tileBuffer = ByteBuffer.allocate(chunk.tiles.size * 4)
                            .order(ByteOrder.BIG_ENDIAN)
                        for (tileId in chunk.tiles) {
                            tileBuffer.putInt(tileId)
                        }
                        raf.write(tileBuffer.array())

                        // Write annotations
                        val annotations = chunk.annotations.cellSet()
                        raf.writeInt(annotations.size)
                        annotations.forEach { cell ->
                            raf.writeInt(cell.rowKey.x)
                            raf.writeInt(cell.rowKey.y)
                            raf.writeInt(cell.rowKey.z)
                            raf.writeString(cell.columnKey)
                            raf.writeMap(cell.value)
                        }
                    }

                    is SparseMapLayer.SparseChunk -> {
                        raf.writeInt(chunk.operations.size)
                        chunk.operations.forEach { (coordinate, operations) ->
                            raf.writeInt(operations.size)
                            operations.forEach { operation ->
                                raf.writeShort(coordinate.x)
                                raf.writeShort(coordinate.y)
                                raf.writeShort(coordinate.z)
                                raf.writeByte(
                                    when (operation) {
                                        is SparseTilePlacement -> OP_PLACEMENT
                                        is SparseTileRemoval -> OP_REMOVAL
                                        is SparseTilesReplacement -> OP_REPLACEMENT
                                        is SparseTileSwap -> OP_SWAP
                                        is SparseTileAnnotation -> OP_ANNOTATION
                                    }
                                )
                                when (operation) {
                                    is SparseTilePlacement -> {
                                        raf.writeInt(operation.tileDef.id)
                                    }

                                    is SparseTileRemoval -> {
                                        raf.writeInt(operation.tileDef.id)
                                    }

                                    is SparseTilesReplacement -> {
                                        raf.writeInt(operation.tileDef.id)
                                    }

                                    is SparseTileSwap -> {
                                        raf.writeInt(operation.oldTileDef.id)
                                        raf.writeInt(operation.newTileDef.id)
                                    }

                                    is SparseTileAnnotation -> {
                                        raf.writeInt(operation.coordinate.x)
                                        raf.writeInt(operation.coordinate.y)
                                        raf.writeInt(operation.coordinate.z)
                                        raf.writeString(operation.key)
                                        raf.writeBoolean(operation.data != null)
                                        operation.data?.let { raf.writeMap(it) }
                                    }
                                }
                            }
                        }
                    }
                }

                val currentPointer = raf.filePointer
                raf.seek(chunkHeaderPointers[chunkIndex])
                raf.writeInt(chunkPointer.toInt())
                raf.writeInt((currentPointer - chunkPointer).toInt())
                raf.seek(currentPointer)
                chunkIndex++
            }
        } else {
            val chunkPointer = raf.filePointer
            raf.writeByte(0)
            val currentPointer = raf.filePointer
            raf.seek(chunkHeaderPointers[chunkIndex])
            raf.writeInt(chunkPointer.toInt())
            raf.writeInt((currentPointer - chunkPointer).toInt())
            raf.seek(currentPointer)
            chunkIndex++
        }
    }

    private fun RandomAccessFile.writeString(str: String) {
        val bytes = str.toByteArray(StandardCharsets.UTF_8)
        writeInt(bytes.size)
        write(bytes)
    }

    private fun ByteBuffer.readString(): String {
        val length = this.int
        val bytes = ByteArray(length)
        this.get(bytes)
        return String(bytes, StandardCharsets.UTF_8)
    }

    private fun RandomAccessFile.writeAny(value: Any?) {
        when (value) {
            null -> {
                writeByte(8)
            }

            is Int -> {
                writeByte(1); writeInt(value)
            }

            is Float -> {
                writeByte(2); writeFloat(value)
            }

            is Boolean -> {
                writeByte(3); writeBoolean(value)
            }

            is Double -> {
                writeByte(4); writeDouble(value)
            }

            is Long -> {
                writeByte(5); writeLong(value)
            }

            is Short -> {
                writeByte(6); writeShort(value.toInt())
            }

            is Byte -> {
                writeByte(7); writeByte(value.toInt())
            }

            is String -> {
                writeByte(0); writeString(value)
            }

            else -> throw RuntimeException("Unsupported annotation value type: ${value::class.qualifiedName}")
        }
    }

    private fun ByteBuffer.readAny(): Any? {
        return when (val type = this.get().toInt()) {
            0 -> readString()
            1 -> this.int
            2 -> this.float
            3 -> this.get().toInt() == 1
            4 -> this.double
            5 -> this.long
            6 -> this.short
            7 -> this.get()
            8 -> null
            else -> throw RuntimeException("Unsupported annotation value type tag: $type")
        }
    }

    private fun RandomAccessFile.writeMap(map: SerializedMap) {
        writeInt(map.size)
        map.forEach { (key, value) ->
            writeString(key)
            writeAny(value)
        }
    }

    private fun ByteBuffer.readMap(): SerializedMap {
        val size = this.int
        val map = mutableMapOf<String, Any?>()
        repeat(size) {
            val key = readString()
            val value = readAny()
            map[key] = value
        }
        return map
    }

    private data class LayerHeader(val type: Int, val chunks: List<ChunkHeader>)
    private data class ChunkHeader(val coordinate: Coordinate, val storageType: Int, val offset: Int, val length: Int)

    companion object {
        private const val LAYER_MAGIC = "SELL"
        private const val LAYER_VERSION = 1
        const val MAGIC = "SELM"
        const val VERSION = 2
        const val STORAGE_INLINE = 1
        const val LAYER_DENSE = 1
        const val LAYER_SPARSE = 2
        const val CHUNK_DENSE = 1
        const val CHUNK_SPARSE = 2
        const val OP_PLACEMENT = 1
        const val OP_REMOVAL = 2
        const val OP_REPLACEMENT = 3
        const val OP_SWAP = 4
        const val OP_ANNOTATION = 5
    }
}
