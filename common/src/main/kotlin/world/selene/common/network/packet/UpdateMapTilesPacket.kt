package world.selene.common.network.packet

import io.netty.buffer.ByteBuf
import world.selene.common.network.Packet
import world.selene.common.network.readCoordinate
import world.selene.common.network.writeCoordinate
import world.selene.common.grid.Coordinate

data class UpdateMapTilesPacket(
    val coordinate: Coordinate,
    val baseTileId: Int,
    val additionalTileIds: List<Int>
) : Packet {
    companion object {
        fun decode(buf: ByteBuf): UpdateMapTilesPacket {
            val coordinate = buf.readCoordinate()
            val baseTileId = buf.readInt()
            val additionalTileCount = buf.readInt()
            val additionalTileIds = mutableListOf<Int>()
            repeat(additionalTileCount) {
                additionalTileIds.add(buf.readInt())
            }
            return UpdateMapTilesPacket(coordinate, baseTileId, additionalTileIds)
        }

        fun encode(buf: ByteBuf, packet: UpdateMapTilesPacket) {
            buf.writeCoordinate(packet.coordinate)
            buf.writeInt(packet.baseTileId)
            buf.writeInt(packet.additionalTileIds.size)
            packet.additionalTileIds.forEach { tileId ->
                buf.writeInt(tileId)
            }
        }
    }
}
