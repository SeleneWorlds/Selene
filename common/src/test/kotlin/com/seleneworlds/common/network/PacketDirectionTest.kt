package com.seleneworlds.common.network

import com.seleneworlds.common.grid.Coordinate
import com.seleneworlds.common.network.packet.DisconnectPacket
import com.seleneworlds.common.network.packet.RequestMovePacket
import io.netty.buffer.Unpooled
import java.io.IOException
import kotlin.test.Test
import kotlin.test.assertFailsWith

class PacketDirectionTest {
    private fun registeredFactory() = PacketFactory().also { PacketRegistrations(it).register() }

    @Test
    fun `server rejects server-to-client packet before decoding its body`() {
        val buf = Unpooled.buffer()
        try {
            buf.writeByte(3) // MapChunkPacket, deliberately with no body

            assertFailsWith<IOException> {
                PacketCodec(
                    registeredFactory(),
                    PacketDirection.CLIENT_TO_SERVER,
                    PacketDirection.SERVER_TO_CLIENT
                ).read(buf)
            }
        } finally {
            buf.release()
        }
    }

    @Test
    fun `client rejects client-to-server packet before decoding its body`() {
        val buf = Unpooled.buffer()
        try {
            buf.writeByte(8) // RequestMovePacket, deliberately with no body

            assertFailsWith<IOException> {
                PacketCodec(
                    registeredFactory(),
                    PacketDirection.SERVER_TO_CLIENT,
                    PacketDirection.CLIENT_TO_SERVER
                ).read(buf)
            }
        } finally {
            buf.release()
        }
    }

    @Test
    fun `server cannot encode a client-to-server packet`() {
        val buf = Unpooled.buffer()
        try {
            assertFailsWith<IOException> {
                registeredFactory().writePacket(
                    buf,
                    PacketDirection.SERVER_TO_CLIENT,
                    RequestMovePacket(Coordinate(1, 2, 3))
                )
            }
        } finally {
            buf.release()
        }
    }

    @Test
    fun `client cannot encode a server-to-client packet`() {
        val buf = Unpooled.buffer()
        try {
            assertFailsWith<IOException> {
                registeredFactory().writePacket(
                    buf,
                    PacketDirection.CLIENT_TO_SERVER,
                    DisconnectPacket("test")
                )
            }
        } finally {
            buf.release()
        }
    }
}
