package world.selene.server.world

import world.selene.server.collision.CollisionResolver
import world.selene.server.dimensions.DimensionManager
import world.selene.server.entities.EntityManager
import world.selene.server.sync.ChunkViewManager

class World(
    val collisionResolver: CollisionResolver,
    val dimensionManager: DimensionManager,
    val entityManager: EntityManager,
    val chunkViewManager: ChunkViewManager
)