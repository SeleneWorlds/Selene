package com.seleneworlds.server.entities

import com.seleneworlds.common.event.EventFactory.arrayBackedEvent
import com.seleneworlds.common.grid.Coordinate

class EntityEvents {
    fun interface EntitySteppedOnTile {
        fun entitySteppedOnTile(entity: EntityApi, coordinate: Coordinate)

        companion object {
            val EVENT = arrayBackedEvent<EntitySteppedOnTile> { listeners ->
                EntitySteppedOnTile { entity, coordinate ->
                    listeners.forEach { it.entitySteppedOnTile(entity, coordinate) }
                }
            }
        }
    }

    fun interface EntitySteppedOffTile {
        fun entitySteppedOffTile(entity: EntityApi, coordinate: Coordinate)

        companion object {
            val EVENT = arrayBackedEvent<EntitySteppedOffTile> { listeners ->
                EntitySteppedOffTile { entity, coordinate ->
                    listeners.forEach { it.entitySteppedOffTile(entity, coordinate) }
                }
            }
        }
    }
}