package com.seleneworlds.client.entity

import com.seleneworlds.client.entity.component.EntityComponent
import com.seleneworlds.common.entities.ComponentConfiguration
import com.seleneworlds.common.grid.Coordinate

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
