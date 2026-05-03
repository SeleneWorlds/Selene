package com.seleneworlds.client.game

import com.seleneworlds.common.event.EventFactory
import com.seleneworlds.common.grid.Coordinate

class ClientEvents {
    fun interface SetupUI {
        fun setupUI()

        companion object {
            val EVENT = EventFactory.arrayBackedEvent<SetupUI> { listeners ->
                SetupUI { listeners.forEach { it.setupUI() } }
            }
        }
    }

    fun interface GamePreTick {
        fun gamePreTick()

        companion object {
            val EVENT = EventFactory.arrayBackedEvent<GamePreTick> { listeners ->
                GamePreTick { listeners.forEach { it.gamePreTick() } }
            }
        }
    }

    fun interface MapChunkChanged {
        fun mapChunkChanged(coordinate: Coordinate, width: Int, height: Int)

        companion object {
            val EVENT = EventFactory.arrayBackedEvent<MapChunkChanged> { listeners ->
                MapChunkChanged { coordinate, width, height ->
                    listeners.forEach { it.mapChunkChanged(coordinate, width, height) }
                }
            }
        }
    }

    fun interface CameraCoordinateChanged {
        fun cameraCoordinateChanged(coordinate: Coordinate)

        companion object {
            val EVENT = EventFactory.arrayBackedEvent<CameraCoordinateChanged> { listeners ->
                CameraCoordinateChanged { coordinate ->
                    listeners.forEach { it.cameraCoordinateChanged(coordinate) }
                }
            }
        }
    }
}