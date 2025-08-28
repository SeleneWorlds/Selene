package world.selene.server.saves

import world.selene.common.util.Coordinate
import world.selene.server.data.Registries
import world.selene.server.maps.ChunkedMapLayer
import world.selene.server.maps.DenseMapLayer
import world.selene.server.maps.MapTree
import world.selene.server.maps.SparseMapLayer
import world.selene.server.maps.SparseTilePlacement
import world.selene.server.maps.SparseTileRemoval
import world.selene.server.maps.SparseTileSwap
import world.selene.server.maps.SparseTilesReplacement
import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.charset.StandardCharsets

class MapTreeFormatBinaryV1(private val registries: Registries) : MapTreeFormat {
    
    override fun load(file: File): MapTree {
        val result = MapTree(registries)
        
        val fileBytes = file.readBytes()
        val buffer = ByteBuffer.wrap(fileBytes).order(ByteOrder.BIG_ENDIAN)
        
        val magic = ByteArray(4)
        buffer.get(magic)
        if (String(magic) != MAGIC) {
            throw RuntimeException("Invalid file format: expected magic '$MAGIC', got '${String(magic)}'")
        }
        
        val version = buffer.short.toInt()
        if (version != VERSION) {
            throw RuntimeException("Unsupported version: expected $VERSION, got $version")
        }
        
        buffer.int // reserved
        val layerCount = buffer.short.toInt()
        
        // Read layer headers
        val layerHeaders = mutableListOf<LayerHeader>()
        repeat(layerCount) {
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
            
            layerHeaders.add(LayerHeader(layerType, chunks))
        }
        
        // Process chunks
        layerHeaders.forEach { layerHeader ->
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
                                    result.placeTile(Coordinate(baseX + dx, baseY + dy, z), tileDef)
                                }
                            }
                        }
                        
                        // Read annotations
                        val annotationCount = buffer.int
                        repeat(annotationCount) {
                            val x = buffer.int
                            val y = buffer.int
                            val z = buffer.int
                            val key = readString(buffer)
                            val data = readMap(buffer)
                            result.annotateTile(Coordinate(x, y, z), key, data)
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
                                        result.placeTile(Coordinate(x, y, z), tileDef)
                                    }
                                    
                                    OP_REMOVAL -> {
                                        val tileId = buffer.int
                                        val tileDef = registries.tiles.get(tileId)
                                            ?: throw RuntimeException("Missing tile definition for id $tileId")
                                        result.removeTile(Coordinate(x, y, z), tileDef)
                                    }
                                    
                                    OP_REPLACEMENT -> {
                                        val tileId = buffer.int
                                        val tileDef = registries.tiles.get(tileId)
                                            ?: throw RuntimeException("Missing tile definition for id $tileId")
                                        result.replaceTiles(Coordinate(x, y, z), tileDef)
                                    }
                                    
                                    OP_SWAP -> {
                                        val oldTileId = buffer.int
                                        val newTileId = buffer.int
                                        val oldTileDef = registries.tiles.get(oldTileId)
                                            ?: throw RuntimeException("Missing tile definition for id $oldTileId")
                                        val newTileDef = registries.tiles.get(newTileId)
                                            ?: throw RuntimeException("Missing tile definition for id $newTileId")
                                        result.swapTile(Coordinate(x, y, z), oldTileDef, newTileDef)
                                    }
                                }
                            }
                        }
                        
                        // Read annotations
                        val annotationCount = buffer.int
                        repeat(annotationCount) {
                            val x = buffer.int
                            val y = buffer.int
                            val z = buffer.int
                            val key = readString(buffer)
                            val data = readMap(buffer)
                            result.annotateTile(Coordinate(x, y, z), key, data)
                        }
                    }
                }
            }
        }
        
        return result
    }

    override fun saveFullyInline(file: File, mapTree: MapTree) {
        RandomAccessFile(file, "rw").use { raf ->
            raf.setLength(0)
            raf.writeBytes(MAGIC)
            raf.writeShort(VERSION)
            raf.writeInt(0) // reserved
            raf.writeShort(mapTree.layers.size)

            val chunkHeaderPointers = mutableListOf<Long>()
            mapTree.layers.forEach { layer ->
                raf.writeByte(
                    when (layer) {
                        is DenseMapLayer -> LAYER_DENSE
                        is SparseMapLayer -> LAYER_SPARSE
                        else -> 0
                    }
                )
                if (layer is ChunkedMapLayer) {
                    raf.writeInt(layer.chunks.size)
                    layer.chunks.forEach { (chunkStart, chunk) ->
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
            }

            var chunkIndex = 0
            mapTree.layers.forEach { layer ->
                if (layer is ChunkedMapLayer) {
                    layer.chunks.forEach { (chunkStart, chunk) ->
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
                                    writeString(raf, cell.columnKey)
                                    writeMap(raf, cell.value)
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
                                        }
                                    }
                                }
                                
                                // Write annotations
                                val annotations = chunk.annotations.cellSet()
                                raf.writeInt(annotations.size)
                                annotations.forEach { cell ->
                                    raf.writeInt(cell.rowKey.x)
                                    raf.writeInt(cell.rowKey.y)
                                    raf.writeInt(cell.rowKey.z)
                                    writeString(raf, cell.columnKey)
                                    writeMap(raf, cell.value)
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
        }
    }
    
    private fun writeString(raf: RandomAccessFile, str: String) {
        val bytes = str.toByteArray(StandardCharsets.UTF_8)
        raf.writeInt(bytes.size)
        raf.write(bytes)
    }
    
    private fun readString(buffer: ByteBuffer): String {
        val length = buffer.int
        val bytes = ByteArray(length)
        buffer.get(bytes)
        return String(bytes, StandardCharsets.UTF_8)
    }
    
    private fun writeMap(raf: RandomAccessFile, map: Map<Any, Any>) {
        raf.writeInt(map.size)
        map.forEach { (key, value) ->
            writeString(raf, key.toString())
            writeString(raf, value.toString())
        }
    }
    
    private fun readMap(buffer: ByteBuffer): Map<Any, Any> {
        val size = buffer.int
        val map = mutableMapOf<Any, Any>()
        repeat(size) {
            val key = readString(buffer)
            val value = readString(buffer)
            map[key] = value
        }
        return map
    }
    
    private data class LayerHeader(val type: Int, val chunks: List<ChunkHeader>)
    private data class ChunkHeader(val coordinate: Coordinate, val storageType: Int, val offset: Int, val length: Int)

    companion object {
        const val MAGIC = "SELM"
        const val VERSION = 2
        const val STORAGE_INLINE = 1
        const val STORAGE_EXTERNAL = 2
        const val LAYER_DENSE = 1
        const val LAYER_SPARSE = 2
        const val CHUNK_DENSE = 1
        const val CHUNK_SPARSE = 2
        const val OP_PLACEMENT = 1
        const val OP_REMOVAL = 2
        const val OP_REPLACEMENT = 3
        const val OP_SWAP = 4
    }
}