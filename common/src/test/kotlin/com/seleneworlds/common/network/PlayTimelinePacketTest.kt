package com.seleneworlds.common.network

import com.seleneworlds.common.network.packet.PlayTimelinePacket
import io.netty.buffer.Unpooled
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class PlayTimelinePacketTest {
    @Test
    fun `rejects invalid transitions`() {
        for (transition in listOf(-1f, Float.NaN, Float.POSITIVE_INFINITY)) {
            assertFailsWith<IllegalArgumentException> {
                PlayTimelinePacket("fog", "example:fog", transition = transition)
            }
        }
    }

    @Test
    fun `round trips all playback parameters`() {
        val expected = PlayTimelinePacket(
            instanceId = "timeline-123",
            timeline = "example:impact",
            parameters = Json.parseToJsonElement(
                """{"position":{"x":12,"y":-4,"z":2},"strength":0.75,"variants":["blue",2]}"""
            ).jsonObject,
            tags = listOf("example:weather", "example:ambient"),
            transition = 2f,
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
        val expected = PlayTimelinePacket("timeline-456", "example:ui_flash")
        val buf = Unpooled.buffer()
        try {
            PlayTimelinePacket.encode(buf, expected)
            assertEquals(expected, PlayTimelinePacket.decode(buf))
        } finally {
            buf.release()
        }
    }
}
