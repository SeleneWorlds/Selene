package com.seleneworlds.server.entities

import com.seleneworlds.common.data.Identifier
import com.seleneworlds.common.data.RegistryReference
import com.seleneworlds.common.entities.ComponentConfiguration
import com.seleneworlds.common.entities.EntityDefinition
import com.seleneworlds.common.grid.Coordinate
import com.seleneworlds.common.grid.Direction
import com.seleneworlds.common.observable.ObservableMap
import com.seleneworlds.common.util.IdResolvable
import com.seleneworlds.common.util.ResolvableReference
import com.seleneworlds.server.attributes.Attribute
import com.seleneworlds.server.attributes.AttributeApi
import com.seleneworlds.server.cameras.viewer.Viewer
import com.seleneworlds.server.dimensions.Dimension
import com.seleneworlds.server.dimensions.DimensionApi
import com.seleneworlds.server.maps.tree.MapTreeApi
import com.seleneworlds.server.players.Player
import com.seleneworlds.server.players.PlayerApi

class EntityApi(val delegate: Entity) : IdResolvable<Int, Entity> {

    fun getNetworkId(): Int {
        return delegate.networkId
    }

    fun getCustomData(identifier: Identifier): Any? {
        return delegate.customData[identifier.toString()]
    }

    fun getCustomDataMap(identifier: Identifier): ObservableMap {
        val key = identifier.toString()
        val value = delegate.customData[key]
        return value as? ObservableMap ?: ObservableMap().also { delegate.customData[key] = it }
    }

    fun setCustomData(identifier: Identifier, value: Any?) {
        delegate.customData[identifier.toString()] = value
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
        delegate.facing = direction
        delegate.dimension?.syncManager?.entityTurned(delegate, direction)
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

    fun spawn(dimension: Dimension?) {
        val targetDimension = dimension ?: delegate.world.dimensionManager.getOrCreateDimension(0)
        delegate.dimension = targetDimension
        targetDimension.syncManager.entityAdded(delegate)
        delegate.world.entityManager.onEntitySpawned(delegate)
    }

    fun despawn() {
        val oldDimension = delegate.dimension
        delegate.world.entityManager.onEntityDespawned(delegate)
        delegate.despawn()
        oldDimension?.syncManager?.entityRemoved(delegate)
    }

    fun remove() {
        delegate.remove()
    }

    fun move(direction: Direction): Boolean {
        val coordinate = delegate.coordinate + direction.vector
        return delegate.moveTo(coordinate)
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
        if (enabled) {
            delegate.visibilityTags.add(tagName)
        } else {
            delegate.visibilityTags.remove(tagName)
        }
    }

    fun isVisible(tagName: String = "default"): Boolean {
        return delegate.visibilityTags.contains(tagName)
    }

    fun isInvisible(tagName: String = "default"): Boolean {
        return !delegate.visibilityTags.contains(tagName)
    }

    fun makeVisible(tagName: String = "default") {
        delegate.visibilityTags.add(tagName)
    }

    fun makeInvisible(tagName: String = "default") {
        delegate.visibilityTags.remove(tagName)
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

    fun addDynamicComponent(name: String, callback: (Player) -> ComponentConfiguration?) {
        delegate.dynamicComponents[name] = object : Entity.ComponentResolver {
            override fun resolveForPlayer(player: Player): ComponentConfiguration? {
                return callback(player)
            }
        }
    }

    fun getControllingPlayers(): List<PlayerApi> {
        return delegate.getControllingPlayers().map { it.api }
    }

    fun getAttribute(name: String): AttributeApi? {
        return delegate.attributes[name]?.api
    }

    fun createAttribute(name: String, initialValue: Any?): AttributeApi {
        val attribute = Attribute(delegate, name, initialValue)
        delegate.attributes[name] = attribute
        return attribute.api
    }

    fun hasTag(tag: String): Boolean {
        return delegate.entityDefinition.tags.contains(tag)
    }

    fun playAnimation(animationName: String) {
        delegate.playAnimation(animationName)
    }

    override fun resolvableReference(): ResolvableReference<Int, Entity> {
        return delegate.resolvableReference()
    }

}
