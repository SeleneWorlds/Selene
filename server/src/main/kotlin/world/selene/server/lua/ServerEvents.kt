package world.selene.server.lua

import world.selene.common.event.EventFactory.arrayBackedEvent

class ServerEvents {
    fun interface ServerStarted {
        fun serverStarted()

        companion object {
            val EVENT = arrayBackedEvent<ServerStarted> { listeners ->
                ServerStarted { listeners.forEach { it.serverStarted() } }
            }
        }
    }

    fun interface ServerReloaded {
        fun serverReloaded()

        companion object {
            val EVENT = arrayBackedEvent<ServerReloaded> { listeners ->
                ServerReloaded { listeners.forEach { it.serverReloaded() } }
            }
        }
    }

}
