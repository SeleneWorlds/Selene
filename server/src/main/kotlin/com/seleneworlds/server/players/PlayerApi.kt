package com.seleneworlds.server.players

import com.seleneworlds.common.grid.Coordinate
import com.seleneworlds.common.util.IdResolvable
import com.seleneworlds.common.util.ResolvableReference
import com.seleneworlds.common.observable.ObservableMap
import com.seleneworlds.server.dimensions.Dimension
import com.seleneworlds.server.entities.EntityApi

class PlayerApi(val delegate: Player) : IdResolvable<String, Player> {

    fun getCustomData(): ObservableMap {
        return delegate.customData
    }

    fun getIdleTime(): Int {
        return delegate.idleTime
    }

    fun getUserId(): String? {
        return delegate.userId
    }

    fun getLocaleString(): String {
        return delegate.localeString
    }

    fun getLanguageString(): String {
        return delegate.languageString
    }

    fun getControlledEntity(): EntityApi? {
        return delegate.controlledEntity?.api
    }

    fun setControlledEntity(entity: EntityApi) {
        delegate.controlledEntity = entity.entity
    }

    fun getCameraEntity(): EntityApi? {
        return delegate.cameraEntity?.api
    }

    fun setCameraEntity(entity: EntityApi) {
        delegate.cameraEntity = entity.entity
    }

    fun setCameraToFollowControlledEntity() {
        delegate.setCameraToFollowControlledEntity()
    }

    fun setCameraToFollowTarget() {
        delegate.setCameraToFollowTarget()
    }

    fun setCameraToCoordinate(dimension: Dimension?, coordinate: Coordinate) {
        delegate.setCameraToCoordinate(dimension ?: delegate.camera.dimension ?: delegate.dimensionManager.getOrCreateDimension(0), coordinate)
    }

    override fun resolvableReference(): ResolvableReference<String, Player> {
        return delegate.resolvableReference()
    }
}
