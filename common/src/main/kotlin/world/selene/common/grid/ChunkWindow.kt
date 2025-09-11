package world.selene.common.grid

import kotlin.math.abs

data class ChunkWindow(val x: Int, val y: Int, val z: Int, val width: Int, val height: Int) {
    fun contains(coordinate: Coordinate): Boolean {
        return coordinate.x >= x && coordinate.x < x + width &&
                coordinate.y >= y && coordinate.y < y + height &&
                coordinate.z == z
    }

    fun isInRange(coordinate: Coordinate, viewRange: Int, verticalViewRange: Int): Boolean {
        val chunkX = Math.floorDiv(coordinate.x, width) * width
        val chunkY = Math.floorDiv(coordinate.y, height) * height
        val chunkZ = coordinate.z
        val windowChunkX = x
        val windowChunkY = y
        val windowChunkZ = z
        return (abs((chunkX - windowChunkX) / width) <= viewRange
                && abs((chunkY - windowChunkY) / height) <= viewRange
                && abs(chunkZ - windowChunkZ) <= verticalViewRange)
    }

    companion object {
        fun around(coordinate: Coordinate, chunkSize: Int, viewRange: Int, verticalViewRange: Int): List<ChunkWindow> {
            val chunkX = Math.floorDiv(coordinate.x, chunkSize) * chunkSize
            val chunkY = Math.floorDiv(coordinate.y, chunkSize) * chunkSize
            val chunkZ = coordinate.z
            val windows = mutableListOf<ChunkWindow>()
            for (dz in 0..verticalViewRange) {
                for (dx in -viewRange..viewRange) {
                    for (dy in -viewRange..viewRange) {
                        windows.add(
                            ChunkWindow(
                                chunkX + dx * chunkSize,
                                chunkY + dy * chunkSize,
                                chunkZ + dz,
                                chunkSize,
                                chunkSize
                            )
                        )
                    }
                }
            }
            return windows
        }

        fun at(coordinate: Coordinate, chunkSize: Int): ChunkWindow {
            val chunkX = Math.floorDiv(coordinate.x, chunkSize) * chunkSize
            val chunkY = Math.floorDiv(coordinate.y, chunkSize) * chunkSize
            return ChunkWindow(chunkX, chunkY, coordinate.z, chunkSize, chunkSize)
        }
    }
}