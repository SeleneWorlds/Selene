package world.selene.server.entities

import world.selene.common.data.RegistryReference
import world.selene.common.entities.ComponentConfiguration
import world.selene.common.entities.EntityDefinition
import world.selene.common.grid.Direction
import world.selene.common.lua.*
import world.selene.common.network.packet.EntityAnimationPacket
import world.selene.common.observable.ObservableMap
import world.selene.common.grid.Coordinate
import world.selene.common.util.ResolvableReference
import world.selene.server.attributes.Attribute
import world.selene.server.cameras.viewer.Viewer
import world.selene.server.data.Registries
import world.selene.server.dimensions.Dimension
import world.selene.server.maps.layers.MapLayer
import world.selene.server.players.Player
import world.selene.server.world.World

class Entity(val registries: Registries, val world: World) : IdResolvable<Int, Entity> {
    val api = EntityApi(this)
    val impassable: Boolean = true
    var networkId: Int = -1
    var entityDefinition: RegistryReference<EntityDefinition> = RegistryReference.unbound()
    var name = "John Selene"
    var coordinate = Coordinate(0, 0, 0)
    var facing: Direction? = null
    var dimension: Dimension? = null
    val map get() = dimension?.mapTree
    val customData = ObservableMap()
    val attributes = mutableMapOf<String, Attribute<*>>()
    val dynamicComponents = mutableMapOf<String, ComponentResolver>()

    val transient get() = networkId == -1

    val visionViewer = object : Viewer {
        override fun canView(layer: MapLayer): Boolean {
            return layer.visibilityTags.any { visionTags.contains(it) }
        }

        override fun canView(entity: Entity): Boolean {
            return entity.visibilityTags.any { visionTags.contains(it) }
        }
    }
    val collisionViewer = object : Viewer {
        override fun canView(layer: MapLayer): Boolean {
            return layer.collisionTags.any { collisionTags.contains(it) }
        }

        override fun canView(entity: Entity): Boolean {
            return entity.collisionTags.any { collisionTags.contains(it) }
        }
    }

    interface ComponentResolver {
        fun resolveForPlayer(player: Player): ComponentConfiguration?
    }

    val visibilityTags = mutableSetOf("default")
    val visionTags = mutableSetOf("default")
    val collisionTags = mutableSetOf("default")

    override fun resolvableReference(): ResolvableReference<Int, Entity> {
        return ResolvableReference(Entity::class, networkId, world.entityManager)
    }

    fun resolveComponentsFor(player: Player): Map<String, ComponentConfiguration> {
        val components = mutableMapOf<String, ComponentConfiguration>()
        dynamicComponents.forEach { (key, value) ->
            value.resolveForPlayer(player)?.let {
                components[key] = it
            }
        }
        return components
    }

    fun turnTo(facing: Direction) {
        this.facing = facing
        val dimension = dimension ?: return
        dimension.syncManager.entityTurned(this, facing)
    }

    fun moveTo(coordinate: Coordinate): Boolean {
        this.facing = world.grid.getDirection(this.coordinate, coordinate)
        val dimension = dimension ?: return false
        if (world.collisionResolver.collidesAt(dimension, collisionViewer, coordinate)) {
            return false
        }
        val prevCoordinate = this.coordinate
        this.coordinate = coordinate
        dimension.syncManager.entityMoved(this, prevCoordinate, coordinate, 0.2f)
        EntityEvents.EntitySteppedOffTile.EVENT.invoker().entitySteppedOffTile(this, prevCoordinate)
        EntityEvents.EntitySteppedOnTile.EVENT.invoker().entitySteppedOnTile(this, coordinate)
        return true
    }

    fun despawn() {
        dimension = null
    }

    fun remove() {
        world.entityManager.removeEntity(this)
    }

    fun getControllingPlayers(): List<Player> {
        return world.playerManager.players.filter { player ->
            player.controlledEntity == this
        }
    }

    override fun toString(): String {
        return "Entity($networkId, $name, ${entityDefinition.identifier})"
    }

    fun playAnimation(animationName: String) {
        dimension?.syncManager?.sendToAllWatching(networkId, EntityAnimationPacket(networkId, animationName))
    }
}
