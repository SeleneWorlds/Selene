package world.selene.client.entity

import world.selene.client.entity.component.EntityComponent
import world.selene.client.entity.component.rendering.IsoVisualComponent
import world.selene.client.entity.component.rendering.ReloadableVisualComponent
import world.selene.client.entity.component.rendering.Visual2DComponent
import world.selene.common.entities.ComponentConfiguration
import world.selene.common.grid.Coordinate

class EntityApi(val entity: Entity) {

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

    fun getLuaComponent(name: String): Any? {
        return when (val component = getComponent(name)) {
            is Visual2DComponent -> component.api
            is IsoVisualComponent -> component.api
            is ReloadableVisualComponent -> component.api
            else -> null
        }
    }

}
