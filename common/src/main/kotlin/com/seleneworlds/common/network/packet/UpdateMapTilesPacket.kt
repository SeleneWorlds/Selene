package com.seleneworlds.common.network.packet

import io.netty.buffer.ByteBuf
import com.seleneworlds.common.network.Packet
import com.seleneworlds.common.network.readCoordinate
import com.seleneworlds.common.network.writeCoordinate
import com.seleneworlds.common.grid.Coordinate

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
