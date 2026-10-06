package com.seleneworlds.server.entities.component

import com.seleneworlds.server.entities.Entity
import com.seleneworlds.server.entities.EntityEvents

/** Makes an entity fall one level per server update until it occupies a map tile. */
class GravityComponent : EntityComponent, TickableComponent {
    private var fallHeight = 0

    override fun update(entity: Entity, delta: Float) {
        val dimension = entity.dimension ?: return
        val resolver = entity.world.collisionResolver
        val coordinate = entity.coordinate
        val onTile = resolver.hasTileAt(dimension, entity.collisionViewer, coordinate)

        if (onTile) {
            finishFall(entity)
            return
        }

        if (entity.fallOneTile()) {
            fallHeight++
            val destination = entity.coordinate
            if (resolver.hasTileAt(dimension, entity.collisionViewer, destination)) {
                finishFall(entity)
            }
        }
    }

    private fun finishFall(entity: Entity) {
        if (fallHeight == 0) return
        EntityEvents.EntityFell.EVENT.invoker().entityFell(entity.api, fallHeight)
        fallHeight = 0
    }
}
