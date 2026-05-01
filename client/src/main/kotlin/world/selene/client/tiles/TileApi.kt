package world.selene.client.tiles

import party.iroiro.luajava.Lua
import world.selene.client.rendering.visual.ReloadableVisualApi
import world.selene.common.data.RegistryReference
import world.selene.common.grid.Coordinate
import world.selene.common.lua.LuaMetatable
import world.selene.common.lua.LuaMetatableProvider
import world.selene.common.tiles.TileDefinition

class TileApi(val tile: Tile) : LuaMetatableProvider {

    fun getCoordinate(): Coordinate {
        return tile.coordinate
    }

    fun getDefinition(): RegistryReference<TileDefinition> {
        return tile.tileDefinition
    }

    fun getVisual(): ReloadableVisualApi {
        return tile.visual.api
    }

    fun getX(): Int {
        return tile.x
    }

    fun getY(): Int {
        return tile.y
    }

    fun getZ(): Int {
        return tile.z
    }

    fun getName(): String {
        return tile.tileDefinition.identifier.toString()
    }

    override fun luaMetatable(lua: Lua): LuaMetatable {
        return TileLuaApi.luaMeta
    }
}
