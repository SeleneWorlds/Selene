package com.seleneworlds.common.network

import com.seleneworlds.common.network.packet.PlayTimelinePacket
import io.netty.buffer.Unpooled
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals

class PlayTimelinePacketTest {
    @Test
    fun `round trips all playback parameters`() {
        val expected = PlayTimelinePacket(
            timeline = "example:impact",
            parameters = Json.parseToJsonElement(
                """{"position":{"x":12,"y":-4,"z":2},"strength":0.75,"variants":["blue",2]}"""
            ).jsonObject,
        )
        val factory = PacketFactory().also { PacketRegistrations(it).register() }
        val buf = Unpooled.buffer()
        try {
            factory.writePacket(buf, PacketDirection.SERVER_TO_CLIENT, expected)
            val packetId = buf.readUnsignedByte().toInt()
            assertEquals(expected, factory.readPacket(packetId, PacketDirection.SERVER_TO_CLIENT, buf))
            assertEquals(0, buf.readableBytes())
        } finally {
            buf.release()
        }
    }

    @Test
    fun `round trips absent optional parameters`() {
        val expected = PlayTimelinePacket("example:ui_flash")
        val buf = Unpooled.buffer()
        try {
            PlayTimelinePacket.encode(buf, expected)
            assertEquals(expected, PlayTimelinePacket.decode(buf))
        } finally {
            buf.release()
        }
    }
}
