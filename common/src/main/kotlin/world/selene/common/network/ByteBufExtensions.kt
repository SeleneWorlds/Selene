package world.selene.common.network

import io.netty.buffer.ByteBuf
import world.selene.common.util.Coordinate
import java.util.UUID

fun ByteBuf.writeUniqueId(uuid: UUID) {
    writeLong(uuid.mostSignificantBits)
    writeLong(uuid.leastSignificantBits)
}

fun ByteBuf.readUniqueId(): UUID {
    val mostSignificantBits = readLong()
    val leastSignificantBits = readLong()
    return UUID(mostSignificantBits, leastSignificantBits)
}

fun ByteBuf.writeCoordinate(coordinate: Coordinate) {
    writeInt(coordinate.x)
    writeInt(coordinate.y)
    writeInt(coordinate.z)
}

fun ByteBuf.writeRelativeCoordinate(baseX: Int, baseY: Int, coordinate: Coordinate) {
    writeByte(coordinate.x - baseX)
    writeByte(coordinate.y - baseY)
    writeInt(coordinate.z)
}

fun ByteBuf.readCoordinate(): Coordinate {
    val x = readInt()
    val y = readInt()
    val z = readInt()
    return Coordinate(x, y, z)
}

fun ByteBuf.readRelativeCoordinate(baseX: Int, baseY: Int): Coordinate {
    val x = baseX + readByte().toInt()
    val y = baseY + readByte().toInt()
    val z = readInt()
    return Coordinate(x, y, z)
}

fun ByteBuf.writeString(string: String) {
    val bytes = string.toByteArray()
    writeShort(bytes.size)
    writeBytes(bytes)
}

fun ByteBuf.readString(): String {
    val length = readShort().toInt()
    val bytes = ByteArray(length)
    readBytes(bytes)
    return String(bytes)
}