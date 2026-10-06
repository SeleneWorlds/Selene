package com.seleneworlds.server.pathfinding

import party.iroiro.luajava.Lua
import party.iroiro.luajava.value.LuaValue
import com.seleneworlds.common.lua.LuaModule
import com.seleneworlds.common.lua.util.checkCoordinate
import com.seleneworlds.common.lua.util.checkInt
import com.seleneworlds.common.lua.util.checkUserdata
import com.seleneworlds.common.lua.util.register
import com.seleneworlds.server.entities.EntityApi

class PathfindingLuaApi(
    private val api: PathfindingApi
) : LuaModule {
    override val name = "selene.pathfinding"

    override fun register(table: LuaValue) {
        table.register("findPath", this::findPath)
    }

    private fun findPath(lua: Lua): Int {
        val entity = lua.checkUserdata<EntityApi>(1)
        val (goal, index) = lua.checkCoordinate(2)
        val searchRadius = if (lua.top > index && lua.isNumber(index + 1)) lua.checkInt(index + 1) else Pathfinder.DEFAULT_SEARCH_RADIUS
        val maxExpandedNodes = if (lua.top > index + 1 && lua.isNumber(index + 2)) {
            lua.checkInt(index + 2)
        } else {
            Pathfinder.DEFAULT_MAX_EXPANDED_NODES
        }
        lua.push(api.findPath(entity, goal, searchRadius, maxExpandedNodes), Lua.Conversion.FULL)
        return 1
    }
}
