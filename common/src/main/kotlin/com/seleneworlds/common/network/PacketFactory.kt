package com.seleneworlds.common.network

import io.netty.buffer.ByteBuf
import java.io.IOException
import kotlin.reflect.KClass

class PacketFactory {
    private data class Registration(
        val directions: Set<PacketDirection>,
        val decoder: (ByteBuf) -> Packet,
        val encoder: (ByteBuf, Packet) -> Unit
    )

    private val registrationsById = mutableMapOf<Int, Registration>()
    private val packetToRegistration = mutableMapOf<KClass<out Packet>, Pair<Int, Registration>>()

    fun <T : Packet> registerPacket(
        packetType: Int,
        directions: Set<PacketDirection>,
        clazz: KClass<out T>,
        encoder: (ByteBuf, T) -> Unit,
        decoder: (ByteBuf) -> T
    ) {
        require(directions.isNotEmpty()) { "Packet $clazz must have at least one direction" }
        check(!registrationsById.containsKey(packetType)) {
            "Could not register $clazz: packet id $packetType is already occupied"
        }

        @Suppress("UNCHECKED_CAST")
        val registration = Registration(directions, decoder, encoder as (ByteBuf, Packet) -> Unit)
        registrationsById[packetType] = registration
        packetToRegistration[clazz] = packetType to registration
    }

    fun readPacket(id: Int, direction: PacketDirection, buf: ByteBuf): Packet? {
        val registration = registrationsById[id] ?: return null
        if (direction !in registration.directions) {
            throw IOException("Packet id $id is not allowed in direction $direction")
        }
        return registration.decoder(buf)
    }

    fun writePacket(buf: ByteBuf, direction: PacketDirection, msg: Packet) {
        val (packetId, registration) = packetToRegistration[msg::class]
            ?: throw IllegalStateException("Packet $msg has not been registered.")
        if (direction !in registration.directions) {
            throw IOException("Packet ${msg::class.simpleName} is not allowed in direction $direction")
        }

        buf.writeByte(packetId)
        registration.encoder(buf, msg)
    }
}
