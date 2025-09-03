package world.selene.server.entities

import party.iroiro.luajava.Lua
import world.selene.common.data.ComponentConfiguration
import world.selene.common.data.EntityDefinition
import world.selene.common.grid.Grid
import world.selene.common.lua.ClosureTrace
import world.selene.common.lua.LuaMappedMetatable
import world.selene.common.lua.LuaMetatable
import world.selene.common.lua.LuaMetatableProvider
import world.selene.common.lua.LuaReferencable
import world.selene.common.lua.LuaReference
import world.selene.common.lua.ManagedLuaTable
import world.selene.common.lua.checkBoolean
import world.selene.common.lua.checkCoordinate
import world.selene.common.lua.checkDirection
import world.selene.common.lua.checkString
import world.selene.common.lua.checkUserdata
import world.selene.common.lua.getCallerInfo
import world.selene.common.lua.toAny
import world.selene.common.lua.toAnyMap
import world.selene.common.lua.toUserdata
import world.selene.common.lua.xpCall
import world.selene.common.network.packet.EntityAnimationPacket
import world.selene.common.util.Coordinate
import world.selene.server.attribute.Attribute
import world.selene.server.lua.Scripting
import world.selene.server.world.World
import world.selene.server.cameras.Viewer
import world.selene.server.data.Registries
import world.selene.server.dimensions.Dimension
import world.selene.server.maps.MapLayer
import world.selene.server.objectMapper
import world.selene.server.player.Player

class Entity(val registries: Registries, val world: World, val scripting: Scripting) : LuaMetatableProvider,
    LuaReferencable<Int, Entity> {
    val impassable: Boolean = true
    var networkId: Int = -1
    lateinit var entityDefinition: EntityDefinition
    var name = "John Selene"
    var coordinate = Coordinate(0, 0, 0)
    var facing: Grid.Direction? = null
    var dimension: Dimension? = null
    val map get() = dimension?.mapTree
    val customData = ManagedLuaTable()
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

    override fun luaReference(): LuaReference<Int, Entity> {
        return LuaReference(Entity::class, networkId, world.entityManager)
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

    fun turnTo(facing: Grid.Direction) {
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
        scripting.signals.entitySteppedOffTile.emit {
            it.push(this, Lua.Conversion.NONE)
            it.push(prevCoordinate, Lua.Conversion.NONE)
            2
        }
        scripting.signals.entitySteppedOnTile.emit {
            it.push(this, Lua.Conversion.NONE)
            it.push(coordinate, Lua.Conversion.NONE)
            2
        }
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

    override fun luaMetatable(lua: Lua): LuaMetatable {
        return luaMeta
    }

    override fun toString(): String {
        return "Entity($networkId, $name, ${entityDefinition.name})"
    }

    private fun playAnimation(animationName: String) {
        dimension?.syncManager?.sendToAllWatching(networkId, EntityAnimationPacket(networkId, animationName))
    }

    companion object {
        private fun luaRef(lua: Lua): Int {
            val entity = lua.checkUserdata<Entity>(1)
            lua.push(entity.luaReference(), Lua.Conversion.NONE)
            return 1
        }

        private fun luaSetCoordinate(lua: Lua): Int {
            val entity = lua.checkUserdata<Entity>(1)
            val (coordinate, _) = lua.checkCoordinate(2)
            entity.coordinate = coordinate
            entity.dimension?.syncManager?.entityTeleported(entity)
            return 0
        }

        private fun luaSpawn(lua: Lua): Int {
            val entity = lua.checkUserdata<Entity>(1)
            val dimension = lua.toUserdata<Dimension>(2) ?: entity.world.dimensionManager.getOrCreateDimension(0)
            entity.dimension = dimension
            dimension.syncManager.entityAdded(entity)
            return 0
        }

        private fun luaDespawn(lua: Lua): Int {
            val entity = lua.checkUserdata<Entity>(1)
            entity.despawn()
            return 0
        }

        private fun luaRemove(lua: Lua): Int {
            val entity = lua.checkUserdata<Entity>(1)
            entity.remove()
            return 0
        }

        private fun luaSetFacing(lua: Lua): Int {
            val entity = lua.checkUserdata<Entity>(1)
            val direction = lua.checkDirection(2, entity.world.grid)
            entity.facing = direction
            entity.dimension?.syncManager?.entityTurned(entity, direction)
            return 0
        }

        private fun luaMove(lua: Lua): Int {
            val entity = lua.checkUserdata<Entity>(1)
            val direction = lua.checkDirection(2, entity.world.grid)
            val coordinate = entity.coordinate + direction.vector
            lua.push(entity.moveTo(coordinate))
            return 1
        }

        private fun luaSetVision(lua: Lua): Int {
            val entity = lua.checkUserdata<Entity>(1)
            val enabled = lua.checkBoolean(2)
            val tagName = if (lua.isString(3)) lua.checkString(3) else "default"
            if (enabled) {
                entity.visionTags.add(tagName)
            } else {
                entity.visionTags.remove(tagName)
            }
            return 0
        }

        private fun luaHasVision(lua: Lua): Int {
            val entity = lua.checkUserdata<Entity>(1)
            val tagName = if (lua.isString(2)) lua.checkString(2) else "default"
            lua.push(entity.visionTags.contains(tagName))
            return 1
        }

        private fun luaGrantVision(lua: Lua): Int {
            val entity = lua.checkUserdata<Entity>(1)
            val tagName = if (lua.isString(2)) lua.checkString(2) else "default"
            entity.visionTags.add(tagName)
            return 0
        }

        private fun luaRevokeVision(lua: Lua): Int {
            val entity = lua.checkUserdata<Entity>(1)
            val tagName = if (lua.isString(2)) lua.checkString(2) else "default"
            entity.visionTags.remove(tagName)
            return 0
        }

        private fun luaSetVisibility(lua: Lua): Int {
            val entity = lua.checkUserdata<Entity>(1)
            val enabled = lua.checkBoolean(2)
            val tagName = if (lua.isString(3)) lua.checkString(3) else "default"
            if (enabled) {
                entity.visibilityTags.add(tagName)
            } else {
                entity.visibilityTags.remove(tagName)
            }
            return 0
        }

        private fun luaIsVisible(lua: Lua): Int {
            val entity = lua.checkUserdata<Entity>(1)
            val tagName = if (lua.isString(2)) lua.checkString(2) else "default"
            lua.push(entity.visibilityTags.contains(tagName))
            return 1
        }

        private fun luaIsInvisible(lua: Lua): Int {
            val entity = lua.checkUserdata<Entity>(1)
            val tagName = if (lua.isString(2)) lua.checkString(2) else "default"
            lua.push(!entity.visibilityTags.contains(tagName))
            return 1
        }

        private fun luaMakeVisible(lua: Lua): Int {
            val entity = lua.checkUserdata<Entity>(1)
            val tagName = if (lua.isString(2)) lua.checkString(2) else "default"
            entity.visibilityTags.add(tagName)
            return 0
        }

        private fun luaMakeInvisible(lua: Lua): Int {
            val entity = lua.checkUserdata<Entity>(1)
            val tagName = if (lua.isString(2)) lua.checkString(2) else "default"
            entity.visibilityTags.remove(tagName)
            return 0
        }

        private fun luaHasCollisions(lua: Lua): Int {
            val entity = lua.checkUserdata<Entity>(1)
            val tagName = if (lua.isString(2)) lua.checkString(2) else "default"
            lua.push(entity.collisionTags.contains(tagName))
            return 1
        }

        private fun luaSetCollisions(lua: Lua): Int {
            val entity = lua.checkUserdata<Entity>(1)
            val enabled = lua.checkBoolean(2)
            val tagName = if (lua.isString(3)) lua.checkString(3) else "default"
            if (enabled) {
                entity.collisionTags.add(tagName)
            } else {
                entity.collisionTags.remove(tagName)
            }
            return 0
        }

        private fun luaEnableCollisions(lua: Lua): Int {
            val entity = lua.checkUserdata<Entity>(1)
            val tagName = if (lua.isString(2)) lua.checkString(2) else "default"
            entity.collisionTags.add(tagName)
            return 0
        }

        private fun luaDisableCollisions(lua: Lua): Int {
            val entity = lua.checkUserdata<Entity>(1)
            val tagName = if (lua.isString(2)) lua.checkString(2) else "default"
            entity.collisionTags.remove(tagName)
            return 0
        }

        private fun luaAddDynamicComponent(lua: Lua): Int {
            val entity = lua.checkUserdata<Entity>(1)
            val name = lua.checkString(2)
            lua.pushValue(3)
            val callback = lua.get()
            val registrationSite = lua.getCallerInfo()
            entity.dynamicComponents[name] = object : ComponentResolver {
                override fun resolveForPlayer(player: Player): ComponentConfiguration? {
                    val lua = callback.state()
                    lua.push(callback)
                    lua.push(entity, Lua.Conversion.NONE)
                    lua.push(player, Lua.Conversion.NONE)
                    lua.xpCall(2, 1, ClosureTrace { "[dynamic component \"$name\"] registered in $registrationSite" })
                    return objectMapper.convertValue(lua.toAnyMap(-1), ComponentConfiguration::class.java)
                }
            }
            return 0
        }

        private fun luaGetControllingPlayers(lua: Lua): Int {
            val entity = lua.checkUserdata<Entity>(1)
            lua.push(entity.getControllingPlayers(), Lua.Conversion.FULL)
            return 1
        }

        private fun luaGetAttribute(lua: Lua): Int {
            val entity = lua.checkUserdata<Entity>(1)
            val name = lua.checkString(2)
            val attribute = entity.attributes[name]
            lua.push(attribute, Lua.Conversion.NONE)
            return 1
        }

        private fun luaCreateAttribute(lua: Lua): Int {
            val entity = lua.checkUserdata<Entity>(1)
            val name = lua.checkString(2)
            val initialValue = lua.toAny(3)
            val attribute = Attribute(entity, name, initialValue)
            entity.attributes[name] = attribute
            lua.push(attribute, Lua.Conversion.NONE)
            return 1
        }

        private fun luaHasTag(lua: Lua): Int {
            val entity = lua.checkUserdata<Entity>(1)
            val tag = lua.checkString(2)
            lua.push(entity.entityDefinition.tags.contains(tag))
            return 1
        }

        private fun luaPlayAnimation(lua: Lua): Int {
            val entity = lua.checkUserdata<Entity>(1)
            val animationName = lua.checkString(2)
            entity.playAnimation(animationName)
            return 0
        }

        val luaMeta = LuaMappedMetatable(Entity::class) {
            readOnly(Entity::networkId)
            readOnly(Entity::customData)
            readOnly(Entity::entityDefinition)
            writable(Entity::name)
            readOnly(Entity::coordinate)
            readOnly(Entity::facing)
            readOnly(Entity::dimension)
            readOnly(Entity::map)
            readOnly(Entity::collisionViewer, "Collision")
            readOnly(Entity::visionViewer, "Vision")
            callable(::luaSpawn)
            callable(::luaDespawn)
            callable(::luaRemove)
            callable(::luaRef)
            callable(::luaSetCoordinate)
            callable(::luaSetFacing)
            callable(::luaMove)
            callable(::luaSetVision)
            callable(::luaHasVision)
            callable(::luaGrantVision)
            callable(::luaRevokeVision)
            callable(::luaSetVisibility)
            callable(::luaIsVisible)
            callable(::luaIsInvisible)
            callable(::luaMakeVisible)
            callable(::luaMakeInvisible)
            callable(::luaHasCollisions)
            callable(::luaSetCollisions)
            callable(::luaEnableCollisions)
            callable(::luaDisableCollisions)
            callable(::luaAddDynamicComponent)
            callable(::luaGetControllingPlayers)
            callable(::luaGetAttribute)
            callable(::luaCreateAttribute)
            callable(::luaHasTag)
            callable(::luaPlayAnimation)
        }
    }

}
