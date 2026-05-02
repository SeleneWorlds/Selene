package world.selene.server.players

import world.selene.common.grid.Coordinate
import world.selene.common.util.IdResolvable
import world.selene.common.util.ResolvableReference
import world.selene.common.observable.ObservableMap
import world.selene.server.dimensions.Dimension
import world.selene.server.entities.EntityApi

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
