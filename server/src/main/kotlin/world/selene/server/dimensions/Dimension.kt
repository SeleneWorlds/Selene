package world.selene.server.dimensions

import party.iroiro.luajava.Lua
import world.selene.common.tiles.TileDefinition
import world.selene.common.lua.*
import world.selene.common.lua.util.checkAnyMap
import world.selene.common.lua.util.checkCoordinate
import world.selene.common.lua.util.checkInt
import world.selene.common.lua.util.checkRegistry
import world.selene.common.lua.util.checkString
import world.selene.common.lua.util.checkUserdata
import world.selene.common.grid.Coordinate
import world.selene.server.cameras.viewer.DefaultViewer
import world.selene.server.cameras.viewer.Viewer
import world.selene.server.data.Registries
import world.selene.server.entities.Entity
import world.selene.server.maps.tree.MapTree
import world.selene.server.maps.tree.MapTreeListener
import world.selene.server.tiles.TransientTile
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

    private fun getAnnotationAt(coordinate: Coordinate, key: String, viewer: Viewer = DefaultViewer): Map<*, *>? {
        val chunkView = world.chunkViewManager.atCoordinate(this, viewer, coordinate)
        return chunkView.getAnnotationAt(coordinate, key)
    }

    @Suppress("SameReturnValue")
    companion object {
        /**
         * Map layer tree for this dimension.
         *
         * ```property
         * MapTree: MapTree
         * ```
         */
        private fun luaGetMap(lua: Lua): Int {
            val dimension = lua.checkUserdata<Dimension>(1)
            lua.push(dimension.mapTree, Lua.Conversion.NONE)
            return 1
        }

        /**
         * ```property
         * MapTree: MapTree
         * ```
         */
        private fun luaSetMap(lua: Lua): Int {
            val dimension = lua.checkUserdata<Dimension>(1)
            dimension.mapTree = lua.checkUserdata<MapTree>(3)
            return 0
        }

        /**
         * Checks if a specific tile exists at the given coordinate.
         *
         * ```signatures
         * HasTile(coordinate: Coordinate, tileDef: TileDefinition) -> boolean
         * HasTile(coordinate: Coordinate, tileDef: TileDefinition, viewer: Viewer) -> boolean
         * ```
         */
        private fun luaHasTile(lua: Lua): Int {
            val dimension = lua.checkUserdata<Dimension>(1)
            val (coordinate, index) = lua.checkCoordinate(2)
            val tile = lua.checkRegistry(index + 1, dimension.registries.tiles)
            val viewer = if (lua.isUserdata(index + 2)) lua.checkUserdata<Viewer>(index + 2) else DefaultViewer
            val chunkView = dimension.world.chunkViewManager.atCoordinate(dimension, viewer, coordinate)
            val baseTile = chunkView.getBaseTileAt(coordinate)
            if (baseTile == tile.id) {
                lua.push(true)
            } else {
                lua.push(chunkView.getAdditionalTilesAt(coordinate).contains(tile.id))
            }
            return 1
        }

        /**
         * Places a tile at the specified coordinate and returns a TransientTile reference.
         *
         * ```signatures
         * PlaceTile(coordinate: Coordinate, tileDef: TileDefinition) -> TransientTile
         * PlaceTile(coordinate: Coordinate, tileDef: TileDefinition, layerName: string) -> TransientTile
         * ```
         */
        private fun luaPlaceTile(lua: Lua): Int {
            val dimension = lua.checkUserdata<Dimension>(1)
            val (coordinate, index) = lua.checkCoordinate(2)
            val tileDef = lua.checkRegistry(index + 1, dimension.registries.tiles)
            val layerName = lua.toString(index + 2)
            dimension.mapTree.placeTile(coordinate, tileDef, layerName)
            lua.push(TransientTile(tileDef, dimension, coordinate), Lua.Conversion.NONE)
            return 1
        }

        /**
         * Adds annotation data to a tile at the specified coordinate.
         *
         * ```signatures
         * AnnotateTile(coordinate: Coordinate, key: string, data: table)
         * AnnotateTile(coordinate: Coordinate, key: string, data: table, layerName: string)
         * ```
         */
        private fun luaAnnotateTile(lua: Lua): Int {
            val dimension = lua.checkUserdata<Dimension>(1)
            val (coordinate, index) = lua.checkCoordinate(2)
            val key = lua.checkString(index + 1)
            val data = lua.checkAnyMap(index + 2)
            val layerName = lua.toString(index + 3)
            dimension.mapTree.annotateTile(coordinate, key, data, layerName)
            return 0
        }

        /**
         * Gets all tiles at the specified coordinate as TransientTile objects.
         *
         * ```signatures
         * GetTilesAt(coordinate: Coordinate) -> table[TransientTile]
         * GetTilesAt(coordinate: Coordinate, viewer: Viewer) -> table[TransientTile]
         * ```
         */
        private fun luaGetTilesAt(lua: Lua): Int {
            val dimension = lua.checkUserdata<Dimension>(1)
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
            return 1
        }

        /**
         * Gets annotation data for a tile at the specified coordinate.
         *
         * ```signatures
         * GetAnnotationAt(coordinate: Coordinate, key: string) -> table|nil
         * GetAnnotationAt(coordinate: Coordinate, key: string, viewer: Viewer) -> table|nil
         * ```
         */
        private fun luaGetAnnotationAt(lua: Lua): Int {
            val dimension = lua.checkUserdata<Dimension>(1)
            val (coordinate, index) = lua.checkCoordinate(2)
            val key = lua.checkString(index + 1)
            val viewer = if (lua.isUserdata(index + 2)) lua.checkUserdata<Viewer>(index + 2) else DefaultViewer
            val annotation = dimension.getAnnotationAt(coordinate, key, viewer)
            lua.push(annotation, Lua.Conversion.FULL)
            return 1
        }

        /**
         * Checks if there is a collision at the specified coordinate for the given viewer.
         *
         * ```signatures
         * HasCollisionAt(coordinate: Coordinate) -> boolean
         * HasCollisionAt(coordinate: Coordinate, viewer: Viewer) -> boolean
         * ```
         */
        private fun luaHasCollisionAt(lua: Lua): Int {
            val dimension = lua.checkUserdata<Dimension>(1)
            val (coordinate, index) = lua.checkCoordinate(2)
            val viewer = if (lua.isUserdata(index + 1)) lua.checkUserdata<Viewer>(index + 1) else DefaultViewer
            lua.push(dimension.world.collisionResolver.collidesAt(dimension, viewer, coordinate))
            return 1
        }

        /**
         * Gets all entities at the specified coordinate.
         *
         * ```signatures
         * GetEntitiesAt(coordinate: Coordinate) -> table[Entity]
         * ```
         */
        private fun luaGetEntitiesAt(lua: Lua): Int {
            val dimension = lua.checkUserdata<Dimension>(1)
            val (coordinate, _) = lua.checkCoordinate(2)
            lua.push(dimension.getEntitiesAt(coordinate), Lua.Conversion.FULL)
            return 1
        }

        /**
         * Gets all entities within the specified range of a coordinate.
         *
         * ```signatures
         * GetEntitiesInRange(coordinate: Coordinate, range: number) -> table[Entity]
         * ```
         */
        private fun luaGetEntitiesInRange(lua: Lua): Int {
            val dimension = lua.checkUserdata<Dimension>(1)
            val (coordinate, index) = lua.checkCoordinate(2)
            val range = lua.checkInt(index + 1)
            lua.push(dimension.getEntitiesInRange(coordinate, range), Lua.Conversion.FULL)
            return 1
        }

        val luaMeta = LuaMappedMetatable(Dimension::class) {
            getter(::luaGetMap)
            setter(::luaSetMap)
            callable(::luaHasTile)
            callable(::luaPlaceTile)
            callable(::luaAnnotateTile)
            callable(::luaGetTilesAt)
            callable(::luaGetAnnotationAt)
            callable(::luaHasCollisionAt)
            callable(::luaGetEntitiesAt)
            callable(::luaGetEntitiesInRange)
        }
    }

}

