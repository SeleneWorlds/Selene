@file:Suppress("RedundantSuppression")
package world.selene.common.network.packet

import io.netty.buffer.ByteBuf
import world.selene.common.network.Packet

class FinalizeJoinPacket : Packet {

    companion object {
        fun decode(@Suppress("unused") buf: ByteBuf): FinalizeJoinPacket {
            return FinalizeJoinPacket()
        }

        fun encode(@Suppress("unused") buf: ByteBuf, @Suppress("unused") packet: FinalizeJoinPacket) = Unit
    }
}