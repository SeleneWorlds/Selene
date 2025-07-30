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
}