package com.seleneworlds.common.network

import com.seleneworlds.common.network.packet.HeartbeatPacket
import io.netty.channel.embedded.EmbeddedChannel
import io.netty.handler.timeout.IdleStateEvent
import kotlin.test.Test
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertSame

class HeartbeatHandlerTest {
    @Test
    fun `server probes when either direction is idle and consumes replies`() {
        val channel = EmbeddedChannel(HeartbeatHandler(server = true))
        try {
            for (event in listOf(IdleStateEvent.READER_IDLE_STATE_EVENT, IdleStateEvent.WRITER_IDLE_STATE_EVENT)) {
                channel.pipeline().fireUserEventTriggered(event)
                assertIs<HeartbeatPacket>(channel.readOutbound<Any>())
            }
            channel.writeInbound(HeartbeatPacket())
            assertNull(channel.readInbound<Any>())
            assertNull(channel.readOutbound<Any>())
        } finally {
            channel.finishAndReleaseAll()
        }
    }

    @Test
    fun `client immediately echoes probes and passes game packets through`() {
        val channel = EmbeddedChannel(HeartbeatHandler(server = false))
        try {
            channel.writeInbound(HeartbeatPacket())
            assertIs<HeartbeatPacket>(channel.readOutbound<Any>())
            assertNull(channel.readInbound<Any>())
            val packet = object : Packet {}
            channel.writeInbound(packet)
            assertSame(packet, channel.readInbound<Any>())
        } finally {
            channel.finishAndReleaseAll()
        }
    }
}
