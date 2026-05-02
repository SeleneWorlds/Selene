package com.seleneworlds.client.entity

import party.iroiro.luajava.Lua
import com.seleneworlds.common.entities.ComponentConfiguration
import com.seleneworlds.common.lua.LuaMappedMetatable
import com.seleneworlds.common.lua.util.checkCoordinate
import com.seleneworlds.common.lua.util.checkString
import com.seleneworlds.common.lua.util.checkUserdata
import com.seleneworlds.common.lua.util.toAnyMap
import com.seleneworlds.common.script.ExposedApi

object EntityLuaApi {

    /**
     * Coordinate this entity is located at.
     *
     * ```property
     * Coordinate: Coordinate
     * ```
     */
    private fun luaGetCoordinate(lua: Lua): Int {
        val self = lua.checkUserdata<EntityApi>(1)
        lua.push(self.getCoordinate(), Lua.Conversion.NONE)
        return 1
    }

    /**
     * Spawns the entity on the client map, making it visible.
     *
     * ```signatures
     * Spawn()
     * ```
     */
    private fun luaSpawn(lua: Lua): Int {
        val self = lua.checkUserdata<EntityApi>(1)
        self.spawn()
        return 0
    }

    /**
     * Despawns the entity from the client map, making it invisible.
     *
     * ```signatures
     * Despawn()
     * ```
     */
    private fun luaDespawn(lua: Lua): Int {
        val self = lua.checkUserdata<EntityApi>(1)
        self.despawn()
        return 0
    }

    /**
     * Sets the entity's coordinate and updates its position on the client.
     *
     * ```signatures
     * SetCoordinate(coordinate: Coordinate)
     * ```
     */
    private fun luaSetCoordinate(lua: Lua): Int {
        val self = lua.checkUserdata<EntityApi>(1)
        val (coordinate, _) = lua.checkCoordinate(2)
        self.setCoordinate(coordinate)
        return 0
    }

    /**
     * Adds a component to the entity with the given configuration.
     *
     * ```signatures
     * AddComponent(name: string, componentData: table)
     * ```
     */
    private fun luaAddComponent(lua: Lua): Int {
        val self = lua.checkUserdata<EntityApi>(1)
        val componentName = lua.checkString(2)
        val componentData = lua.toAnyMap(3)
        val componentConfiguration =
            self.entity.objectMapper.convertValue(componentData, ComponentConfiguration::class.java)
        self.addComponent(componentName, componentConfiguration)
        return 0
    }

    /**
     * Gets a component by name from the entity.
     *
     * ```signatures
     * GetComponent(name: string) -> EntityComponent|nil
     * ```
     */
    private fun luaGetComponent(lua: Lua): Int {
        val self = lua.checkUserdata<EntityApi>(1)
        val componentName = lua.checkString(2)
        val component = self.getComponent(componentName)
        if (component is ExposedApi<*>) {
            lua.push(component.api, Lua.Conversion.NONE)
            return 1
        }
        return 0
    }

    val luaMeta = LuaMappedMetatable(EntityApi::class) {
        getter(::luaGetCoordinate)
        callable(::luaSpawn)
        callable(::luaDespawn)
        callable(::luaSetCoordinate)
        callable(::luaAddComponent)
        callable(::luaGetComponent)
    }
}
