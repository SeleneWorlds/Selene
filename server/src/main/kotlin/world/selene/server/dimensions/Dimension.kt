package world.selene.server.dimensions

import party.iroiro.luajava.Lua
import world.selene.common.data.TileDefinition
import world.selene.common.lua.LuaMappedMetatable
import world.selene.common.lua.LuaMetatable
import world.selene.common.lua.LuaMetatableProvider
import world.selene.common.lua.checkCoordinate
import world.selene.common.lua.checkInt
import world.selene.common.lua.checkUserdata
import world.selene.common.lua.checkRegistry
import world.selene.server.cameras.DefaultViewer
import world.selene.server.cameras.Viewer
import world.selene.server.data.Registries
import world.selene.server.maps.MapTree
import world.selene.server.maps.MapTreeListener
import world.selene.server.maps.TransientTile
import world.selene.common.util.Coordinate
import world.selene.server.entities.Entity
import world.selene.server.sync.DimensionSyncManager
import world.selene.server.world.World

class Dimension(val registries: Registries, val world: World) : MapTreeListener, LuaMetatableProvider {
    var mapTree: MapTree = MapTree(registries).also { it.addListener(this) }
        set(value) {
            field.removeListener(this)
            field = value
            value.addListener(this)
        }
    val syncManager = DimensionSyncManager()

    override fun onTileUpdated(coordinate: Coordinate) {
        syncManager.tileUpdated(coordinate)
    }

    override fun luaMetatable(lua: Lua): LuaMetatable {
        return luaMeta
    }

    fun swapTile(
        coordinate: Coordinate,
        oldTileDef: TileDefinition,
        newTileDef: TileDefinition,
        layerName: String?
    ): TransientTile {
        return if (mapTree.swapTile(coordinate, oldTileDef, newTileDef, layerName)) {
            TransientTile(newTileDef, this, coordinate)
        } else {
            TransientTile(oldTileDef, this, coordinate)
        }
    }

    fun getEntitiesAt(coordinate: Coordinate): List<Entity> {
        return world.entityManager.getEntitiesAt(coordinate, this)
    }

    fun getEntitiesInRange(coordinate: Coordinate, range: Int): List<Entity> {
        return world.entityManager.getNearbyEntities(coordinate, this, range)
    }

    companion object {
        val luaMeta = LuaMappedMetatable(Dimension::class) {
            writable(Dimension::mapTree, "Map")
            callable("HasTile") {
                val dimension = it.checkSelf()
                val (coordinate, index) = it.checkCoordinate(2)
                val tile = it.checkRegistry(index + 1, dimension.registries.tiles)
                val viewer = if (it.isUserdata(index + 2)) it.checkUserdata<Viewer>(index + 2) else DefaultViewer
                val chunkView = dimension.world.chunkViewManager.atCoordinate(dimension, viewer, coordinate)
                val baseTile = chunkView.getBaseTileAt(coordinate)
                if (baseTile == tile.id) {
                    it.push(true)
                    return@callable 1
                }

                it.push(chunkView.getAdditionalTilesAt(coordinate).contains(tile.id))
                1
            }
            callable("PlaceTile") { lua ->
                val dimension = lua.checkSelf()
                val (coordinate, index) = lua.checkCoordinate(2)
                val tileDef = lua.checkRegistry(index + 1, dimension.registries.tiles)
                val layerName = lua.toString(index + 2)
                dimension.mapTree.placeTile(coordinate, tileDef, layerName)
                lua.push(TransientTile(tileDef, dimension, coordinate), Lua.Conversion.NONE)
                return@callable 1
            }
            callable("GetTilesAt") { lua ->
                val dimension = lua.checkSelf()
                val (coordinate, index) = lua.checkCoordinate(2)
                val viewer = if (lua.isUserdata(index + 1)) lua.checkUserdata<Viewer>(index + 1) else DefaultViewer

                val tiles = mutableListOf<TransientTile>()
                val chunkView = dimension.world.chunkViewManager.atCoordinate(dimension, viewer, coordinate)
                val baseTileId = chunkView.getBaseTileAt(coordinate)
                val baseTile = dimension.registries.tiles.get(baseTileId)
                if (baseTile != null) {
                    tiles.add(TransientTile(baseTile, dimension, coordinate))
                }
                val additionalTiles = chunkView.getAdditionalTilesAt(coordinate)
                additionalTiles.forEach { tileId ->
                    val tile = dimension.registries.tiles.get(tileId)
                    if (tile != null) {
                        tiles.add(TransientTile(tile, dimension, coordinate))
                    }
                }
                lua.push(tiles, Lua.Conversion.FULL)
                1
            }
            callable("HasCollisionAt") { lua ->
                val dimension = lua.checkSelf()
                val (coordinate, index) = lua.checkCoordinate(2)
                val viewer = if (lua.isUserdata(index + 1)) lua.checkUserdata<Viewer>(index + 1) else DefaultViewer
                lua.push(dimension.world.collisionResolver.collidesAt(dimension, viewer, coordinate))
                1
            }
            callable("GetEntitiesAt") {
                val dimension = it.checkUserdata<Dimension>(1)
                val (coordinate, _) = it.checkCoordinate(2)
                it.push(dimension.getEntitiesAt(coordinate), Lua.Conversion.FULL)
                1
            }
            callable("GetEntitiesInRange") {
                val dimension = it.checkUserdata<Dimension>(1)
                val (coordinate, index) = it.checkCoordinate(2)
                val range = it.checkInt(index + 1)
                it.push(dimension.getEntitiesInRange(coordinate, range), Lua.Conversion.FULL)
                1
            }
        }
    }
}

