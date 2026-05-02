@file:Suppress("RedundantSuppression")

package com.seleneworlds.common.network.packet

import io.netty.buffer.ByteBuf
import com.seleneworlds.common.network.Packet

class FinalizeJoinPacket : Packet {

    companion object {
        fun decode(@Suppress("unused") buf: ByteBuf): FinalizeJoinPacket {
            return FinalizeJoinPacket()
        }

        fun encode(@Suppress("unused") buf: ByteBuf, @Suppress("unused") packet: FinalizeJoinPacket) = Unit
    }
}