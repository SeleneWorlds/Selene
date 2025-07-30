package world.selene.common.network.packet

import io.netty.buffer.ByteBuf
import world.selene.common.network.Packet
import world.selene.common.network.readString
import world.selene.common.network.writeString

data class NameIdMappingsPacket(
    val scope: String,
    val mappings: List<Map.Entry<String, Int>>
) : Packet {
    companion object {
        fun decode(buf: ByteBuf): NameIdMappingsPacket {
            val scope = buf.readString()
            val mappings = mutableMapOf<String, Int>()
            val entryCount = buf.readInt()
            repeat(entryCount) {
                val name = buf.readString()
                val id = buf.readInt()
                mappings.put(name, id)
            }
            return NameIdMappingsPacket(scope, mappings.entries.toList())
        }

        fun encode(buf: ByteBuf, packet: NameIdMappingsPacket) {
            buf.writeString(packet.scope)
            buf.writeInt(packet.mappings.size)
            for ((name, id) in packet.mappings) {
                buf.writeString(name)
                buf.writeInt(id)
            }
        }
    }
}