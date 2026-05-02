package com.seleneworlds.client.entity

import party.iroiro.luajava.Lua
import party.iroiro.luajava.value.LuaValue
import com.seleneworlds.client.data.Registries
import com.seleneworlds.client.entity.component.rendering.IsoVisualComponentApi
import com.seleneworlds.client.entity.component.rendering.IsoVisualComponentLuaApi
import com.seleneworlds.client.entity.component.rendering.ReloadableVisualComponentApi
import com.seleneworlds.client.entity.component.rendering.ReloadableVisualComponentLuaApi
import com.seleneworlds.client.entity.component.rendering.Visual2DComponentApi
import com.seleneworlds.client.entity.component.rendering.Visual2DComponentLuaApi
import com.seleneworlds.common.lua.LuaManager
import com.seleneworlds.common.lua.LuaModule
import com.seleneworlds.common.lua.util.checkCoordinate
import com.seleneworlds.common.lua.util.checkRegistry
import com.seleneworlds.common.lua.util.checkType
import com.seleneworlds.common.lua.util.getFieldString
import com.seleneworlds.common.lua.util.register

/**
 * Create and lookup entities.
 */
class EntitiesLuaApi(
    private val api: EntitiesApi,
    private val registries: Registries
) : LuaModule {
    override val name = "selene.entities"

    override fun initialize(luaManager: LuaManager) {
        luaManager.defineMetatable(EntityApi::class, EntityLuaApi.luaMeta)
        luaManager.defineMetatable(Visual2DComponentApi::class, Visual2DComponentLuaApi.luaMeta)
        luaManager.defineMetatable(IsoVisualComponentApi::class, IsoVisualComponentLuaApi.luaMeta)
        luaManager.defineMetatable(ReloadableVisualComponentApi::class, ReloadableVisualComponentLuaApi.luaMeta)
    }

    override fun register(table: LuaValue) {
        table.register("Create", this::luaCreate)
        table.register("GetEntitiesAt", this::luaGetEntitiesAt)
        table.register("FindEntitiesAt", this::luaFindEntitiesAt)
    }

    private fun luaCreate(lua: Lua): Int {
        lua.push(api.create(lua.checkRegistry(1, registries.entities)), Lua.Conversion.NONE)
        return 1
    }

    private fun luaGetEntitiesAt(lua: Lua): Int {
        val (coordinate) = lua.checkCoordinate(1)
        lua.push(api.getEntitiesAt(coordinate), Lua.Conversion.FULL)
        return 1
    }

    private fun luaFindEntitiesAt(lua: Lua): Int {
        val (coordinate, index) = lua.checkCoordinate(1)
        lua.checkType(index + 1, Lua.LuaType.TABLE)
        lua.push(api.findEntitiesAt(coordinate, lua.getFieldString(index + 1, "tag")), Lua.Conversion.FULL)
        return 1
    }
}
