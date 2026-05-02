package com.seleneworlds.server.players

import com.seleneworlds.common.event.EventFactory.arrayBackedEvent
import com.seleneworlds.server.login.LoginQueueEntry
import com.seleneworlds.server.login.LoginQueueStatus

class PlayerEvents {
    fun interface PlayerQueued {
        fun playerQueued(entry: LoginQueueEntry): LoginQueueStatus

        companion object {
            val EVENT = arrayBackedEvent<PlayerQueued> { listeners ->
                PlayerQueued { player ->
                    var status = LoginQueueStatus.Accepted
                    listeners.forEach {
                        status = it.playerQueued(player)
                    }
                    return@PlayerQueued status
                }
            }
        }
    }

    fun interface PlayerDequeued {
        fun playerDequeued(entry: LoginQueueEntry)

        companion object {
            val EVENT = arrayBackedEvent<PlayerDequeued> { listeners ->
                PlayerDequeued { player -> listeners.forEach { it.playerDequeued(player) } }
            }
        }
    }

    fun interface PlayerJoined {
        fun playerJoined(player: PlayerApi)

        companion object {
            val EVENT = arrayBackedEvent<PlayerJoined> { listeners ->
                PlayerJoined { player -> listeners.forEach { it.playerJoined(player) } }
            }
        }
    }

    fun interface PlayerLeft {
        fun playerLeft(player: PlayerApi)

        companion object {
            val EVENT = arrayBackedEvent<PlayerLeft> { listeners ->
                PlayerLeft { player -> listeners.forEach { it.playerLeft(player) } }
            }
        }
    }
}
