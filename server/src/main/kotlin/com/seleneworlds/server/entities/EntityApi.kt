package com.seleneworlds.server.entities

import com.seleneworlds.common.data.RegistryReference
import com.seleneworlds.common.entities.ComponentConfiguration
import com.seleneworlds.common.entities.EntityDefinition
import com.seleneworlds.common.grid.Coordinate
import com.seleneworlds.common.grid.Direction
import com.seleneworlds.common.util.IdResolvable
import com.seleneworlds.common.util.ResolvableReference
import com.seleneworlds.common.observable.ObservableMap
import com.seleneworlds.server.attributes.Attribute
import com.seleneworlds.server.attributes.AttributeApi
import com.seleneworlds.server.cameras.viewer.Viewer
import com.seleneworlds.server.dimensions.Dimension
import com.seleneworlds.server.dimensions.DimensionApi
import com.seleneworlds.server.maps.tree.MapTreeApi
import com.seleneworlds.server.players.Player
import com.seleneworlds.server.players.PlayerApi

class EntityApi(val entity: Entity) : IdResolvable<Int, Entity> {

    fun getNetworkId(): Int {
        return entity.networkId
    }

    fun getCustomData(): ObservableMap {
        return entity.customData
    }

    fun getEntityDefinition(): RegistryReference<EntityDefinition> {
        return entity.entityDefinition
    }

    fun getName(): String {
        return entity.name
    }

    fun setName(name: String) {
        entity.name = name
    }

    fun getCoordinate(): Coordinate {
        return entity.coordinate
    }

    fun setCoordinate(coordinate: Coordinate) {
        entity.coordinate = coordinate
        entity.dimension?.syncManager?.entityTeleported(entity)
    }

    fun getFacing(): Direction? {
        return entity.facing
    }

    fun setFacing(direction: Direction) {
        entity.facing = direction
        entity.dimension?.syncManager?.entityTurned(entity, direction)
    }

    fun getDimension(): DimensionApi? {
        return entity.dimension?.api
    }

    fun getMap(): MapTreeApi? {
        return entity.map?.api

    }

    fun getCollisionViewer(): Viewer {
        return entity.collisionViewer
    }

    fun getVisionViewer(): Viewer {
        return entity.visionViewer
    }

    fun spawn(dimension: Dimension?) {
        val targetDimension = dimension ?: entity.world.dimensionManager.getOrCreateDimension(0)
        entity.dimension = targetDimension
        targetDimension.syncManager.entityAdded(entity)
    }

    fun despawn() {
        entity.despawn()
    }

    fun remove() {
        entity.remove()
    }

    fun move(direction: Direction): Boolean {
        val coordinate = entity.coordinate + direction.vector
        return entity.moveTo(coordinate)
    }

    fun setVision(enabled: Boolean, tagName: String = "default") {
        if (enabled) {
            entity.visionTags.add(tagName)
        } else {
            entity.visionTags.remove(tagName)
        }
    }

    fun hasVision(tagName: String = "default"): Boolean {
        return entity.visionTags.contains(tagName)
    }

    fun grantVision(tagName: String = "default") {
        entity.visionTags.add(tagName)
    }

    fun revokeVision(tagName: String = "default") {
        entity.visionTags.remove(tagName)
    }

    fun setVisibility(enabled: Boolean, tagName: String = "default") {
        if (enabled) {
            entity.visibilityTags.add(tagName)
        } else {
            entity.visibilityTags.remove(tagName)
        }
    }

    fun isVisible(tagName: String = "default"): Boolean {
        return entity.visibilityTags.contains(tagName)
    }

    fun isInvisible(tagName: String = "default"): Boolean {
        return !entity.visibilityTags.contains(tagName)
    }

    fun makeVisible(tagName: String = "default") {
        entity.visibilityTags.add(tagName)
    }

    fun makeInvisible(tagName: String = "default") {
        entity.visibilityTags.remove(tagName)
    }

    fun hasCollisions(tagName: String = "default"): Boolean {
        return entity.collisionTags.contains(tagName)
    }

    fun setCollisions(enabled: Boolean, tagName: String = "default") {
        if (enabled) {
            entity.collisionTags.add(tagName)
        } else {
            entity.collisionTags.remove(tagName)
        }
    }

    fun enableCollisions(tagName: String = "default") {
        entity.collisionTags.add(tagName)
    }

    fun disableCollisions(tagName: String = "default") {
        entity.collisionTags.remove(tagName)
    }

    fun addDynamicComponent(name: String, callback: (Player) -> ComponentConfiguration?) {
        entity.dynamicComponents[name] = object : Entity.ComponentResolver {
            override fun resolveForPlayer(player: Player): ComponentConfiguration? {
                return callback(player)
            }
        }
    }

    fun getControllingPlayers(): List<PlayerApi> {
        return entity.getControllingPlayers().map { it.api }
    }

    fun getAttribute(name: String): AttributeApi? {
        return entity.attributes[name]?.api
    }

    fun createAttribute(name: String, initialValue: Any?): AttributeApi {
        val attribute = Attribute(entity, name, initialValue)
        entity.attributes[name] = attribute
        return attribute.api
    }

    fun hasTag(tag: String): Boolean {
        return entity.entityDefinition.tags.contains(tag)
    }

    fun playAnimation(animationName: String) {
        entity.playAnimation(animationName)
    }

    override fun resolvableReference(): ResolvableReference<Int, Entity> {
        return entity.resolvableReference()
    }
}
