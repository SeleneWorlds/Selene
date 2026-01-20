package world.selene.common.network.packet

import io.netty.buffer.ByteBuf
import world.selene.common.network.Packet
import world.selene.common.network.readString
import world.selene.common.network.writeString

data class NotifyAssetUpdatePacket(
    val bundleId: String,
    val updated: List<String> = emptyList(),
    val deleted: List<String> = emptyList()
) : Packet {
    
    companion object {
        fun decode(buf: ByteBuf): NotifyAssetUpdatePacket {
            val bundleId = buf.readString()

            val updatedCount = buf.readInt()
            val updated = mutableListOf<String>()
            repeat(updatedCount) {
                updated.add(buf.readString())
            }
            
            val deletedCount = buf.readInt()
            val deleted = mutableListOf<String>()
            repeat(deletedCount) {
                deleted.add(buf.readString())
            }
            
            return NotifyAssetUpdatePacket(bundleId, updated, deleted)
        }

        fun encode(buf: ByteBuf, packet: NotifyAssetUpdatePacket) {
            buf.writeString(packet.bundleId)
            
            buf.writeInt(packet.updated.size)
            for (path in packet.updated) {
                buf.writeString(path)
            }

            buf.writeInt(packet.deleted.size)
            for (path in packet.deleted) {
                buf.writeString(path)
            }
        }
    }
}
