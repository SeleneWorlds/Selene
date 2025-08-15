package world.selene.common.network.packet

import io.netty.buffer.ByteBuf
import world.selene.common.network.Packet
import world.selene.common.network.readCoordinate
import world.selene.common.network.readString
import world.selene.common.network.writeCoordinate
import world.selene.common.network.writeString
import world.selene.common.util.Coordinate

data class EntityPacket(
    val networkId: Int,
    val entityId: Int,
    val coordinate: Coordinate,
    val facing: Float,
    val components: Map<String, String> // TODO in the future would be nice to have custom de/encoders
) : Packet {
    companion object {
        fun encode(buf: ByteBuf, packet: EntityPacket) {
            buf.writeInt(packet.networkId)
            buf.writeInt(packet.entityId)
            buf.writeCoordinate(packet.coordinate)
            buf.writeFloat(packet.facing)
            buf.writeInt(packet.components.size)
            packet.components.forEach { (key, value) ->
                buf.writeString(key)
                buf.writeString(value)
            }
        }

        fun decode(buf: ByteBuf): EntityPacket {
            val networkId = buf.readInt()
            val entityId = buf.readInt()
            val coordinate = buf.readCoordinate()
            val facing = buf.readFloat()
            val componentCount = buf.readInt()
            val components = mutableMapOf<String, String>()
            repeat(componentCount) {
                val key = buf.readString()
                val value = buf.readString()
                components[key] = value
            }
            return EntityPacket(networkId, entityId, coordinate, facing, components)
        }
    }
}
