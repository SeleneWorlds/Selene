package com.seleneworlds.client.maps

import com.google.common.collect.ArrayListMultimap
import com.google.common.collect.Multimap
import org.slf4j.Logger
import com.seleneworlds.client.data.Registries
import com.seleneworlds.client.entity.Entity
import com.seleneworlds.client.entity.EntityPool
import com.seleneworlds.client.game.ClientEvents
import com.seleneworlds.client.rendering.scene.Scene
import com.seleneworlds.client.tiles.Tile
import com.seleneworlds.client.tiles.TilePool
import com.seleneworlds.common.entities.ComponentConfiguration
import com.seleneworlds.common.grid.Coordinate

class ClientMap(
    private val logger: Logger,
    private val tilePool: TilePool,
    private val entityPool: EntityPool,
    private val registries: Registries,
    private val scene: Scene
) {
    private val tiles = ArrayListMultimap.create<Coordinate, Tile>()
    private val entitiesByNetworkId = HashMap<Int, Entity>()
    private val entitiesByCoordinate = ArrayListMultimap.create<Coordinate, Entity>()

    fun setChunk(
        x: Int, y: Int, z: Int,
        width: Int, height: Int,
        baseTiles: IntArray,
        additionalTiles: Multimap<Coordinate, Int>
    ) {
        scene.beginBatch()
        for (dy in 0 until height) {
            for (dx in 0 until width) {
                val coordinate = Coordinate(x + dx, y + dy, z)
                resetTiles(coordinate)
                val idx = dy * width + dx
                val tileId = baseTiles[idx]
                if (tileId != 0) {
                    placeTile(coordinate, tileId)
                }
            }
        }
        additionalTiles.forEach { coordinate, tileId ->
            placeTile(coordinate, tileId)
        }
        ClientEvents.MapChunkChanged.EVENT.invoker().mapChunkChanged(Coordinate(x, y, z), width, height)
        scene.endBatch()
    }

    private fun resetTiles(coordinate: Coordinate) {
        val removedTiles = tiles.removeAll(coordinate)
        removedTiles.forEach {
            scene.remove(it)
        }
    }

    fun placeTile(coordinate: Coordinate, tileId: Int) {
        val tileDefinition = registries.tiles.getReference(tileId)
        if (!tileDefinition.valid) {
            logger.error("Unknown tile id: $tileId")
            return
        }

        val tile = tilePool.obtain()
        tile.coordinate = coordinate
        tile.localSortLayer = tiles.get(coordinate).size
        tile.tileDefinition = tileDefinition
        tiles.put(coordinate, tile)
        scene.add(tile)
    }

    fun placeOrUpdateEntity(
        entityId: Int,
        networkId: Int,
        coordinate: Coordinate,
        facing: Float,
        componentOverrides: Map<String, ComponentConfiguration>
    ) {
        val entityDefinition = registries.entities.get(entityId)
        if (entityDefinition == null) {
            logger.error("Unknown entity id: $entityId")
            return
        }
        val entity = entitiesByNetworkId[networkId] ?: entityPool.obtain().also {
            it.entityDefinition = entityDefinition.asReference
            it.networkId = networkId
            it.setCoordinateAndUpdate(coordinate)
            addEntity(it)
        }
        entity.setCoordinateAndUpdate(coordinate)
        entity.facing = facing
        componentOverrides.forEach { (key, value) ->
            entity.addComponent(key, value)
        }
    }

    fun addEntity(entity: Entity) {
        entitiesByCoordinate.put(entity.coordinate, entity)
        if (entity.networkId != -1) {
            entitiesByNetworkId[entity.networkId] = entity
        }
        scene.add(entity)
    }

    fun entityMoved(entity: Entity, oldCoordinate: Coordinate) {
        if (entitiesByCoordinate.remove(oldCoordinate, entity)) {
            entitiesByCoordinate.put(entity.coordinate, entity)
        }
    }

    fun removeEntityByNetworkId(networkId: Int) {
        entitiesByNetworkId[networkId]?.let {
            removeEntity(it)
        }
    }

    fun removeEntity(entity: Entity) {
        entitiesByCoordinate.remove(entity.coordinate, entity)
        entitiesByNetworkId.remove(entity.networkId)
        scene.remove(entity)
    }

    fun getTilesAt(coordinate: Coordinate): List<Tile> {
        return tiles.get(coordinate)
    }

    fun getEntityByNetworkId(networkId: Int): Entity? {
        return entitiesByNetworkId[networkId]
    }

    fun hasTileAt(coordinate: Coordinate): Boolean {
        return tiles.get(coordinate).isNotEmpty()
    }

    fun getEntitiesAt(coordinate: Coordinate): List<Entity> {
        return entitiesByCoordinate.get(coordinate)
    }

    fun updateTile(coordinate: Coordinate, baseTileId: Int, additionalTileIds: List<Int>) {
        resetTiles(coordinate)

        if (baseTileId != 0) {
            placeTile(coordinate, baseTileId)
        }

        additionalTileIds.forEach { tileId ->
            if (tileId != 0) {
                placeTile(coordinate, tileId)
            }
        }

        ClientEvents.MapChunkChanged.EVENT.invoker().mapChunkChanged(coordinate, 1, 1)
    }

    fun removeChunk(x: Int, y: Int, z: Int, width: Int, height: Int) {
        for (dy in 0 until height) {
            for (dx in 0 until width) {
                resetTiles(Coordinate(x + dx, y + dy, z))
            }
        }
    }
}
