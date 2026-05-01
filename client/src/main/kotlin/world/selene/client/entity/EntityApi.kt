package world.selene.client.entity

import party.iroiro.luajava.Lua
import world.selene.client.entity.component.EntityComponent
import world.selene.client.entity.component.rendering.IsoVisualComponent
import world.selene.client.entity.component.rendering.ReloadableVisualComponent
import world.selene.client.entity.component.rendering.Visual2DComponent
import world.selene.common.entities.ComponentConfiguration
import world.selene.common.grid.Coordinate
import world.selene.common.lua.LuaMetatable
import world.selene.common.lua.LuaMetatableProvider

class EntityApi(val entity: Entity) : LuaMetatableProvider {

    fun getCoordinate(): Coordinate {
        return entity.coordinate
    }

    fun spawn() {
        entity.spawn()
    }

    fun despawn() {
        entity.despawn()
    }

    fun setCoordinate(coordinate: Coordinate) {
        entity.setCoordinateAndUpdate(coordinate)
    }

    fun addComponent(name: String, componentConfiguration: ComponentConfiguration) {
        entity.addComponent(name, componentConfiguration)
    }

    fun getComponent(name: String): EntityComponent? {
        return entity.components[name]
    }

    fun getLuaComponent(name: String): LuaMetatableProvider? {
        return when (val component = getComponent(name)) {
            is Visual2DComponent -> component.api
            is IsoVisualComponent -> component.api
            is ReloadableVisualComponent -> component.api
            else -> null
        }
    }

    override fun luaMetatable(lua: Lua): LuaMetatable {
        return EntityLuaApi.luaMeta
    }
}
