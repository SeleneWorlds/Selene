package com.seleneworlds.common.network

import com.seleneworlds.common.network.packet.StopTimelinePacket
import io.netty.buffer.Unpooled
import kotlin.test.Test
import kotlin.test.assertEquals

class StopTimelinePacketTest {
    @Test
    fun `round trips instance target`() {
        assertRoundTrip(StopTimelinePacket(instanceId = "timeline-123"))
    }

    @Test
    fun `round trips timeline type target`() {
        assertRoundTrip(StopTimelinePacket(timeline = "example:rain"))
    }

    private fun assertRoundTrip(expected: StopTimelinePacket) {
        val buffer = Unpooled.buffer()
        try {
            StopTimelinePacket.encode(buffer, expected)
            assertEquals(expected, StopTimelinePacket.decode(buffer))
            assertEquals(0, buffer.readableBytes())
        } finally {
            buffer.release()
        }
    }
}
