package world.selene.client.entity

import world.selene.client.entity.component.EntityComponent
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

}
