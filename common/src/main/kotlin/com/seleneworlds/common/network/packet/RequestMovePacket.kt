package com.seleneworlds.common.network.packet

import io.netty.buffer.ByteBuf
import com.seleneworlds.common.network.Packet
import com.seleneworlds.common.network.readCoordinate
import com.seleneworlds.common.network.writeCoordinate
import com.seleneworlds.common.grid.Coordinate

data class RequestMovePacket(val coordinate: Coordinate) : Packet {
    companion object {
        fun decode(buf: ByteBuf): RequestMovePacket {
            val coordinate = buf.readCoordinate()
            return RequestMovePacket(coordinate)
        }

        fun encode(buf: ByteBuf, packet: RequestMovePacket) {
            buf.writeCoordinate(packet.coordinate)
        }
    }
}