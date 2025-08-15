package world.selene.client.maps

import com.google.common.collect.ArrayListMultimap
import com.google.common.collect.Multimap
import party.iroiro.luajava.Lua
import world.selene.client.grid.Grid
import world.selene.client.lua.ClientLuaSignals
import world.selene.client.scene.Scene
import world.selene.client.visual.VisualManager
import world.selene.common.data.ConfiguredComponent
import world.selene.common.util.Coordinate

class ClientMap(
    private val tilePool: TilePool,
    private val grid: Grid,
    private val entityPool: EntityPool,
    private val scene: Scene,
    private val visualManager: VisualManager,
    private val signals: ClientLuaSignals
) {
    private val tiles = ArrayListMultimap.create<Coordinate, Tile>()
    private val entitiesByNetworkId = HashMap<Int, Entity>()

    // TODO entitiesByCoordinate is never updated when entity is moved
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
        tilePool.freeAll(removedTiles)
    }

    fun placeTile(coordinate: Coordinate, tileId: Int) {
        val tile = tilePool.obtain(tileId)
        tile.coordinate = coordinate
        tile.localSortLayer = tiles.get(coordinate).size
        tiles.put(coordinate, tile)
        scene.add(tile)
    }

    fun placeOrUpdateEntity(
        entityId: Int,
        networkId: Int,
        coordinate: Coordinate,
        facing: Coordinate,
        componentOverrides: Map<String, ConfiguredComponent>
    ) {
        val entity = entitiesByNetworkId[networkId] ?: entityPool.obtain(entityId).also {
            entitiesByCoordinate.put(coordinate, it)
            entitiesByNetworkId[networkId] = it
            scene.add(it)
        }
        entity.networkId = networkId
        entity.setCoordinate(coordinate)
        grid.getDirection(coordinate, facing)?.let { entity.facing = it }
        entity.setupComponents(componentOverrides)
        entity.updateVisual()
    }

    fun removeEntityByNetworkId(networkId: Int) {
        entitiesByNetworkId[networkId]?.let {
            removeEntity(it.coordinate, it)
        }
    }

    fun removeEntity(coordinate: Coordinate, entity: Entity) {
        entitiesByCoordinate.remove(coordinate, entity)
        entitiesByNetworkId.remove(entity.networkId)
        scene.remove(entity)
        entityPool.free(entity)
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

    fun removeChunk(x: Int, y: Int, z: Int, width: Int, height: Int) {
        for (dy in 0 until height) {
            for (dx in 0 until width) {
                resetTiles(Coordinate(x + dx, y + dy, z))
            }
        }
    }
}
