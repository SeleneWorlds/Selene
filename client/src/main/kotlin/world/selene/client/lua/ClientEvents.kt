package world.selene.client.lua

import world.selene.common.event.EventFactory.arrayBackedEvent
import world.selene.common.grid.Coordinate

class ClientEvents {
    fun interface GamePreTick {
        fun gamePreTick()

        companion object {
            val EVENT = arrayBackedEvent<GamePreTick> { listeners ->
                GamePreTick { listeners.forEach { it.gamePreTick() } }
            }
        }
    }

    fun interface MapChunkChanged {
        fun mapChunkChanged(coordinate: Coordinate, width: Int, height: Int)

        companion object {
            val EVENT = arrayBackedEvent<MapChunkChanged> { listeners ->
                MapChunkChanged { coordinate, width, height ->
                    listeners.forEach { it.mapChunkChanged(coordinate, width, height) }
                }
            }
        }
    }

    fun interface CameraCoordinateChanged {
        fun cameraCoordinateChanged(coordinate: Coordinate)

        companion object {
            val EVENT = arrayBackedEvent<CameraCoordinateChanged> { listeners ->
                CameraCoordinateChanged { coordinate ->
                    listeners.forEach { it.cameraCoordinateChanged(coordinate) }
                }
            }
        }
    }
}
