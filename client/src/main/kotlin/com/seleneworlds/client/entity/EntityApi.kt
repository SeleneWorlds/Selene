package com.seleneworlds.client.entity

import com.seleneworlds.client.entity.component.EntityComponent
import com.seleneworlds.common.entities.ComponentConfiguration
import com.seleneworlds.common.grid.Coordinate

class EntityApi(val delegate: Entity) {

    fun getCoordinate(): Coordinate {
        return delegate.coordinate
    }

    fun spawn() {
        delegate.spawn()
    }

    fun despawn() {
        delegate.despawn()
    }

    fun setCoordinate(coordinate: Coordinate) {
        delegate.setCoordinateAndUpdate(coordinate)
    }

    fun addComponent(name: String, componentConfiguration: ComponentConfiguration) {
        delegate.addComponent(name, componentConfiguration)
    }

    fun getComponent(name: String): EntityComponent? {
        return delegate.components[name]
    }

}
