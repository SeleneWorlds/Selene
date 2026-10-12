package com.seleneworlds.server.entities

import com.seleneworlds.common.data.RegistryReference
import com.seleneworlds.common.entities.ComponentConfiguration
import com.seleneworlds.common.entities.EntityDefinition
import com.seleneworlds.common.grid.Coordinate
import com.seleneworlds.common.grid.Direction
import com.seleneworlds.common.util.IdResolvable
import com.seleneworlds.common.util.ResolvableReference
import com.seleneworlds.server.cameras.viewer.Viewer
import com.seleneworlds.server.dimensions.Dimension
import com.seleneworlds.server.dimensions.DimensionApi
import com.seleneworlds.server.entities.component.GravityComponent
import com.seleneworlds.server.maps.tree.MapTreeApi
import com.seleneworlds.server.players.Player
import com.seleneworlds.server.players.PlayerApi

class EntityApi(val delegate: Entity) : IdResolvable<Int, Entity> {

    fun getNetworkId(): Int {
        return delegate.networkId
    }

    fun getEntityDefinition(): RegistryReference<EntityDefinition> {
        return delegate.entityDefinition
    }

    fun getName(): String {
        return delegate.name
    }

    fun setName(name: String) {
        delegate.name = name
    }

    fun getCoordinate(): Coordinate {
        return delegate.coordinate
    }

    fun setCoordinate(coordinate: Coordinate) {
        delegate.coordinate = coordinate
        delegate.dimension?.syncManager?.entityTeleported(delegate)
    }

    fun getFacing(): Direction? {
        return delegate.facing
    }

    fun setFacing(direction: Direction) {
        delegate.turnTo(direction)
    }

    fun getDimension(): DimensionApi? {
        return delegate.dimension?.api
    }

    fun getMap(): MapTreeApi? {
        return delegate.map?.api
    }

    fun getCollisionViewer(): Viewer {
        return delegate.collisionViewer
    }

    fun getVisionViewer(): Viewer {
        return delegate.visionViewer
    }

    fun getInteractionViewer(): Viewer {
        return delegate.interactionViewer
    }

    fun spawn(dimension: Dimension?) {
        val targetDimension = dimension ?: delegate.world.dimensionManager.getOrCreateDimension(0)
        delegate.dimension = targetDimension
        targetDimension.syncManager.entityAdded(delegate)
        delegate.world.entityManager.onEntitySpawned(delegate)
    }

    fun despawn() {
        delegate.despawn()
    }

    fun remove() {
        delegate.remove()
    }

    fun move(direction: Direction): Boolean {
        val coordinate = delegate.coordinate + direction.vector
        return delegate.moveTo(coordinate)
    }

    fun isMoving(): Boolean = delegate.isMoving()

    fun getMovementDurationMultiplier(): Float {
        return delegate.movementDurationMultiplier
    }

    fun setMovementDurationMultiplier(multiplier: Float) {
        delegate.movementDurationMultiplier = multiplier
    }

    fun setVision(enabled: Boolean, tagName: String = "default") {
        if (enabled) {
            delegate.visionTags.add(tagName)
        } else {
            delegate.visionTags.remove(tagName)
        }
    }

    fun hasVision(tagName: String = "default"): Boolean {
        return delegate.visionTags.contains(tagName)
    }

    fun grantVision(tagName: String = "default") {
        delegate.visionTags.add(tagName)
    }

    fun revokeVision(tagName: String = "default") {
        delegate.visionTags.remove(tagName)
    }

    fun setVisibility(enabled: Boolean, tagName: String = "default") {
        val changed = if (enabled) {
            delegate.visibilityTags.add(tagName)
        } else {
            delegate.visibilityTags.remove(tagName)
        }
        if (changed) {
            delegate.updateVisibility()
        }
    }

    fun isVisible(tagName: String = "default"): Boolean {
        return delegate.visibilityTags.contains(tagName)
    }

    fun isInvisible(tagName: String = "default"): Boolean {
        return !delegate.visibilityTags.contains(tagName)
    }

    fun makeVisible(tagName: String = "default") {
        if (delegate.visibilityTags.add(tagName)) {
            delegate.updateVisibility()
        }
    }

    fun makeInvisible(tagName: String = "default") {
        if (delegate.visibilityTags.remove(tagName)) {
            delegate.updateVisibility()
        }
    }

    fun hasCollisions(tagName: String = "default"): Boolean {
        return delegate.collisionTags.contains(tagName)
    }

    fun setCollisions(enabled: Boolean, tagName: String = "default") {
        if (enabled) {
            delegate.collisionTags.add(tagName)
        } else {
            delegate.collisionTags.remove(tagName)
        }
    }

    fun enableCollisions(tagName: String = "default") {
        delegate.collisionTags.add(tagName)
    }

    fun disableCollisions(tagName: String = "default") {
        delegate.collisionTags.remove(tagName)
    }

    fun setGravityEnabled(enabled: Boolean) {
        delegate.components.values.filterIsInstance<GravityComponent>().forEach {
            it.setActive(enabled)
        }
    }

    fun hasInteractions(tagName: String = "default"): Boolean {
        return delegate.interactionTags.contains(tagName)
    }

    fun setInteractions(enabled: Boolean, tagName: String = "default") {
        if (enabled) {
            delegate.interactionTags.add(tagName)
        } else {
            delegate.interactionTags.remove(tagName)
        }
    }

    fun enableInteractions(tagName: String = "default") {
        delegate.interactionTags.add(tagName)
    }

    fun disableInteractions(tagName: String = "default") {
        delegate.interactionTags.remove(tagName)
    }

    fun addDynamicComponent(name: String, callback: (Player) -> ComponentConfiguration?) {
        delegate.dynamicComponents[name] = object : Entity.ComponentResolver {
            override fun resolveForPlayer(player: Player): ComponentConfiguration? {
                return callback(player)
            }
        }
    }

    fun getPrimaryControllingPlayer(): PlayerApi? {
        return delegate.getPrimaryControllingPlayer()?.api
    }

    fun getControllingPlayers(): List<PlayerApi> {
        return delegate.getControllingPlayers().map { it.api }
    }

    fun hasTag(tag: String): Boolean {
        return delegate.entityDefinition.tags.contains(tag)
    }

    fun playAnimation(animationName: String) {
        delegate.playAnimation(animationName)
    }

    fun updateVisuals() {
        delegate.updateVisuals()
    }

    override fun resolvableReference(): ResolvableReference<Int, Entity> {
        return delegate.resolvableReference()
    }

}
