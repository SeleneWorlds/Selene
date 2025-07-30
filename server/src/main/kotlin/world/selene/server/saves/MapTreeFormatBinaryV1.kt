package world.selene.server.saves

import world.selene.server.maps.ChunkedMapLayer
import world.selene.server.maps.DenseMapLayer
import world.selene.server.maps.MapManager
import world.selene.server.maps.MapTree
import world.selene.server.maps.SparseMapLayer
import world.selene.server.maps.SparseTilePlacement
import world.selene.server.maps.SparseTileRemoval
import world.selene.server.maps.SparseTilesReplacement
import java.io.File
import java.io.RandomAccessFile

class MapTreeFormatBinaryV1(private val mapManager: MapManager) : MapTreeFormat {
    override fun load(file: File): MapTree {
        val result = mapManager.createMapTree()
        
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
                        if (chunk is DenseMapLayer.DenseChunk) {
                            for (i in chunk.tiles) {
                                raf.writeInt(i)
                            }
                        } else if (chunk is SparseMapLayer.SparseChunk) {
                            raf.writeInt(chunk.operations.size)
                            chunk.operations.forEach { coordinate, operations ->
                                raf.writeInt(operations.size)
                                operations.forEach { operation ->
                                    raf.writeShort(coordinate.x)
                                    raf.writeShort(coordinate.y)
                                    raf.writeShort(coordinate.z)
                                    raf.writeByte(
                                        when (operation) {
                                            is SparseTilePlacement -> 1
                                            is SparseTileRemoval -> 2
                                            is SparseTilesReplacement -> 3
                                        }
                                    )
                                    when (operation) {
                                        is SparseTilePlacement -> {
                                            raf.writeInt(operation.tileId)
                                        }

                                        is SparseTileRemoval -> {
                                            raf.writeInt(operation.tileId)
                                        }

                                        is SparseTilesReplacement -> {
                                            raf.writeInt(operation.tileId)
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

    companion object {
        const val MAGIC = "SELM"
        const val VERSION = 2
        const val STORAGE_INLINE = 1
        const val STORAGE_EXTERNAL = 2
        const val LAYER_DENSE = 1
        const val LAYER_SPARSE = 2
        const val CHUNK_DENSE = 1
        const val CHUNK_SPARSE = 2
    }
}