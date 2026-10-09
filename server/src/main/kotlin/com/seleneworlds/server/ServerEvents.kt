package com.seleneworlds.server

import com.seleneworlds.common.event.EventFactory

class ServerEvents {
    fun interface BundleFilesChanged {
        fun bundleFilesChanged(bundleId: String, updatedFiles: Set<String>, deletedFiles: Set<String>)

        companion object {
            val EVENT = EventFactory.arrayBackedEvent<BundleFilesChanged> { listeners ->
                BundleFilesChanged { bundleId, updatedFiles, deletedFiles ->
                    listeners.forEach { listener ->
                        EventFactory.catchLog { listener.bundleFilesChanged(bundleId, updatedFiles, deletedFiles) }
                    }
                }
            }
        }
    }

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
