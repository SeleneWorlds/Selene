package com.seleneworlds.common.network.packet

import io.netty.buffer.ByteBuf
import com.seleneworlds.common.network.Packet
import com.seleneworlds.common.network.readCoordinate
import com.seleneworlds.common.network.writeCoordinate
import com.seleneworlds.common.grid.Coordinate

data class SetCameraPositionPacket(val coordinate: Coordinate) : Packet {
    companion object {
        fun decode(buf: ByteBuf): SetCameraPositionPacket {
            val position = buf.readCoordinate()
            return SetCameraPositionPacket(position)
        }

        fun encode(buf: ByteBuf, packet: SetCameraPositionPacket) {
            buf.writeCoordinate(packet.coordinate)
        }
    }
}
