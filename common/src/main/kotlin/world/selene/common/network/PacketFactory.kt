package world.selene.common.network

import io.netty.buffer.ByteBuf
import java.io.IOException
import kotlin.reflect.KClass

class PacketFactory {
    private val packetDecoders = mutableMapOf<Int, (ByteBuf) -> Packet>()
    private val packetEncoders = mutableMapOf<Int, (ByteBuf, Packet) -> Unit>()
    private val packetToId = mutableMapOf<KClass<out Packet>, Int>()

    fun <T : Packet> registerPacket(packetType: Int, clazz: KClass<out T>, encoder: (ByteBuf, T) -> Unit, decoder: (ByteBuf) -> T) {
        check(!packetDecoders.containsKey(packetType)) {
            "Could not register $clazz: packet id $packetType is already occupied by ${packetDecoders[packetType]}"
        }

        packetDecoders[packetType] = decoder
        @Suppress("UNCHECKED_CAST")
        packetEncoders[packetType] = encoder as (ByteBuf, Packet) -> Unit
        packetToId[clazz] = packetType
    }

    private fun getIdForPacket(packet: Packet): Int {
        return packetToId[packet::class] ?: throw IllegalStateException("Packet $packet has not been registered.")
    }

    fun readPacket(id: Int, buf: ByteBuf): Packet? {
        return packetDecoders[id]?.invoke(buf)
    }

    fun writePacket(buf: ByteBuf, msg: Packet) {
        val packetId = getIdForPacket(msg)
        if (packetId == -1) {
            throw IOException("Packet type $msg is not registered in the packet factory.")
        }

        buf.writeByte(packetId)
        packetEncoders[packetId]?.invoke(buf, msg)
    }
}
