package com.seleneworlds.server.players

import com.seleneworlds.common.grid.Coordinate
import com.seleneworlds.common.network.packet.DisconnectPacket
import com.seleneworlds.common.util.IdResolvable
import com.seleneworlds.common.util.ResolvableReference
import com.seleneworlds.server.dimensions.Dimension
import com.seleneworlds.server.entities.EntityApi

class PlayerApi(val delegate: Player) : IdResolvable<String, Player> {

    fun isVisibleOnline(): Boolean = delegate.isVisibleOnline

    fun setVisibleOnline(visibleOnline: Boolean) {
        delegate.isVisibleOnline = visibleOnline
    }

    fun getIdleTime(): Int {
        return delegate.idleTime
    }

    fun setCanMove(canMove: Boolean) {
        delegate.canMove = canMove
    }

    fun canMove(): Boolean = delegate.canMove

    fun setCanTurn(canTurn: Boolean) {
        delegate.canTurn = canTurn
    }

    fun canTurn(): Boolean = delegate.canTurn

    fun getUserId(): String? {
        return delegate.userId
    }

    fun getLocaleString(): String {
        return delegate.localeString
    }

    fun getLanguageString(): String {
        return delegate.languageString
    }

    fun kick(reason: String) {
        delegate.client.send(DisconnectPacket(reason))
        delegate.client.disconnect()
    }

    fun getControlledEntity(): EntityApi? {
        return delegate.controlledEntity?.api
    }

    fun setControlledEntity(entity: EntityApi) {
        delegate.controlledEntity = entity.delegate
    }

    fun getCameraEntity(): EntityApi? {
        return delegate.cameraEntity?.api
    }

    fun setCameraEntity(entity: EntityApi) {
        delegate.cameraEntity = entity.delegate
    }

    fun setCameraZoom(zoom: Float) {
        delegate.camera.zoom = zoom
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
