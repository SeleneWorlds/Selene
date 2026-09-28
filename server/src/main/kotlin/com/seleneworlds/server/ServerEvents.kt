package com.seleneworlds.server

import com.seleneworlds.common.event.EventFactory

class ServerEvents {
    fun interface ServerStarted {
        fun serverStarted()

        companion object {
            val EVENT = EventFactory.arrayBackedEvent<ServerStarted> { listeners ->
                ServerStarted { listeners.forEach { EventFactory.catchLog { it.serverStarted() } } }
            }
        }
    }

    fun interface ServerReloaded {
        fun serverReloaded()

        companion object {
            val EVENT = EventFactory.arrayBackedEvent<ServerReloaded> { listeners ->
                ServerReloaded { listeners.forEach { EventFactory.catchLog { it.serverReloaded() } } }
            }
        }
    }

}
