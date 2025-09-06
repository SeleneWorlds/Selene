package world.selene.common.network.packet

import com.google.common.collect.ArrayListMultimap
import io.netty.buffer.ByteBuf
import world.selene.common.network.Packet
import world.selene.common.network.readRelativeCoordinate
import world.selene.common.network.writeRelativeCoordinate
import world.selene.common.util.Coordinate

data class MapChunkPacket(
    val x: Int,
    val y: Int,
    val z: Int,
    val width: Int,
    val height: Int,
    val padding: Int,
    val baseTiles: IntArray,
    val additionalTiles: ArrayListMultimap<Coordinate, Int>
) : Packet {
    companion object {
        fun decode(buf: ByteBuf): MapChunkPacket {
            val x = buf.readInt()
            val y = buf.readInt()
            val z = buf.readInt()
            val width = buf.readInt()
            val height = buf.readInt()
            val tiles = IntArray(width * height)
            for (i in 0 until width * height) {
                tiles[i] = buf.readInt()
            }
            val additionalTilesCount = buf.readShort().toInt()
            val additionalTiles = ArrayListMultimap.create<Coordinate, Int>()
            for (i in 0 until additionalTilesCount) {
                val coordinate = buf.readRelativeCoordinate(x, y)
                val tile = buf.readInt()
                additionalTiles.put(coordinate, tile)
            }
            return MapChunkPacket(x, y, z, width, height, 0, tiles, additionalTiles)
        }

        fun encode(buf: ByteBuf, packet: MapChunkPacket) {
            buf.writeInt(packet.x)
            buf.writeInt(packet.y)
            buf.writeInt(packet.z)
            buf.writeInt(packet.width)
            buf.writeInt(packet.height)
            for (y in 0 until packet.width) {
                for (x in 0 until packet.height) {
                    buf.writeInt(packet.baseTiles[(x + packet.padding) + (y + packet.padding) * (packet.width + 2 * packet.padding)])
                }
            }
            buf.writeShort(packet.additionalTiles.size())
            for ((coordinate, tile) in packet.additionalTiles.entries()) {
                buf.writeRelativeCoordinate(packet.x, packet.y, coordinate)
                buf.writeInt(tile)
            }
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as MapChunkPacket

        if (x != other.x) return false
        if (y != other.y) return false
        if (z != other.z) return false
        if (width != other.width) return false
        if (height != other.height) return false
        if (padding != other.padding) return false
        if (!baseTiles.contentEquals(other.baseTiles)) return false
        if (additionalTiles != other.additionalTiles) return false

        return true
    }

    override fun hashCode(): Int {
        var result = x
        result = 31 * result + y
        result = 31 * result + z
        result = 31 * result + width
        result = 31 * result + height
        result = 31 * result + padding
        result = 31 * result + baseTiles.contentHashCode()
        result = 31 * result + additionalTiles.hashCode()
        return result
    }
}