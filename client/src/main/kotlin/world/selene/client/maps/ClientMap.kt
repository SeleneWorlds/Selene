package world.selene.client.maps

import com.google.common.collect.ArrayListMultimap
import com.google.common.collect.Multimap
import org.slf4j.Logger
import party.iroiro.luajava.Lua
import world.selene.client.data.Registries
import world.selene.client.lua.ClientLuaSignals
import world.selene.client.scene.Scene
import world.selene.common.data.ComponentConfiguration
import world.selene.common.util.Coordinate

class ClientMap(
    private val logger: Logger,
    private val tilePool: TilePool,
    private val entityPool: EntityPool,
    private val registries: Registries,
    private val scene: Scene,
    private val signals: ClientLuaSignals
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
        signals.mapChunkChanged.emit { lua ->
            lua.push(Coordinate(x, y, z), Lua.Conversion.NONE)
            lua.push(width)
            lua.push(height)
            3
        }
        scene.endBatch()
    }

    private fun resetTiles(coordinate: Coordinate) {
        val removedTiles = tiles.removeAll(coordinate)
        removedTiles.forEach {
            scene.remove(it)
        }
    }

    fun placeTile(coordinate: Coordinate, tileId: Int) {
        val tileDefinition = registries.tiles.get(tileId)
        if (tileDefinition == null) {
            logger.error("Unknown tile id: $tileId")
            return
        }

        val tile = tilePool.obtain()
        tile.tileDefinition = tileDefinition
        tile.coordinate = coordinate
        tile.localSortLayer = tiles.get(coordinate).size
        tile.updateVisual()
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
            it.entityDefinition = entityDefinition
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

        signals.mapChunkChanged.emit { lua ->
            lua.push(coordinate, Lua.Conversion.NONE)
            lua.push(1) // width = 1 for single tile
            lua.push(1) // height = 1 for single tile
            3
        }
    }

    fun removeChunk(x: Int, y: Int, z: Int, width: Int, height: Int) {
        for (dy in 0 until height) {
            for (dx in 0 until width) {
                resetTiles(Coordinate(x + dx, y + dy, z))
            }
        }
    }
}
