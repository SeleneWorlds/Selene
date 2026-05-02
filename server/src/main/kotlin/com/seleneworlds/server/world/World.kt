package com.seleneworlds.server.world

import com.seleneworlds.common.grid.Grid
import com.seleneworlds.server.collision.CollisionResolver
import com.seleneworlds.server.dimensions.DimensionManager
import com.seleneworlds.server.entities.EntityManager
import com.seleneworlds.server.players.PlayerManager
import com.seleneworlds.server.sync.ChunkViewManager

class World(
    val grid: Grid,
    val collisionResolver: CollisionResolver,
    val dimensionManager: DimensionManager,
    val entityManager: EntityManager,
    val chunkViewManager: ChunkViewManager,
    val playerManager: PlayerManager
)