package world.selene.server.entities

import party.iroiro.luajava.Lua
import world.selene.common.data.ComponentConfiguration
import world.selene.common.data.EntityDefinition
import world.selene.common.grid.Direction
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
    var facing: Direction? = null
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
        /**
         * Gets the network ID of this entity.
         *
         * ```property
         * NetworkId: number
         * ```
         */
        private fun luaGetNetworkId(lua: Lua): Int {
            val entity = lua.checkUserdata<Entity>(1)
            lua.push(entity.networkId)
            return 1
        }

        /**
         * Gets the custom data of this entity.
         *
         * ```property
         * CustomData: ManagedLuaTable
         * ```
         */
        private fun luaGetCustomData(lua: Lua): Int {
            val entity = lua.checkUserdata<Entity>(1)
            lua.push(entity.customData, Lua.Conversion.NONE)
            return 1
        }

        /**
         * Gets the entity definition of this entity.
         *
         * ```property
         * EntityDefinition: EntityDefinition
         * ```
         */
        private fun luaGetEntityDefinition(lua: Lua): Int {
            val entity = lua.checkUserdata<Entity>(1)
            lua.push(entity.entityDefinition, Lua.Conversion.NONE)
            return 1
        }

        /**
         * Gets the name of this entity.
         *
         * ```property
         * Name: string
         * ```
         */
        private fun luaGetName(lua: Lua): Int {
            val entity = lua.checkUserdata<Entity>(1)
            lua.push(entity.name)
            return 1
        }

        /**
         * Sets the name of this entity.
         *
         * ```property
         * Name: string
         * ```
         */
        private fun luaSetName(lua: Lua): Int {
            val entity = lua.checkUserdata<Entity>(1)
            val name = lua.checkString(3)
            entity.name = name
            return 0
        }

        /**
         * Gets the coordinate of this entity.
         *
         * ```property
         * Coordinate: Coordinate
         * ```
         */
        private fun luaGetCoordinate(lua: Lua): Int {
            val entity = lua.checkUserdata<Entity>(1)
            lua.push(entity.coordinate, Lua.Conversion.NONE)
            return 1
        }

        /**
         * Gets the facing of this entity.
         *
         * ```property
         * Facing: Direction
         * ```
         */
        private fun luaGetFacing(lua: Lua): Int {
            val entity = lua.checkUserdata<Entity>(1)
            lua.push(entity.facing, Lua.Conversion.NONE)
            return 1
        }

        /**
         * Gets the dimension of this entity.
         *
         * ```property
         * Dimension: Dimension
         * ```
         */
        private fun luaGetDimension(lua: Lua): Int {
            val entity = lua.checkUserdata<Entity>(1)
            lua.push(entity.dimension, Lua.Conversion.NONE)
            return 1
        }

        /**
         * Gets the map of this entity.
         *
         * ```property
         * Map: Map
         * ```
         */
        private fun luaGetMap(lua: Lua): Int {
            val entity = lua.checkUserdata<Entity>(1)
            lua.push(entity.map, Lua.Conversion.NONE)
            return 1
        }

        /**
         * Gets the collision viewer of this entity.
         *
         * ```property
         * CollisionViewer: CollisionViewer
         * ```
         */
        private fun luaGetCollisionViewer(lua: Lua): Int {
            val entity = lua.checkUserdata<Entity>(1)
            lua.push(entity.collisionViewer, Lua.Conversion.NONE)
            return 1
        }

        /**
         * Gets the vision viewer of this entity.
         *
         * ```property
         * VisionViewer: VisionViewer
         * ```
         */
        private fun luaGetVisionViewer(lua: Lua): Int {
            val entity = lua.checkUserdata<Entity>(1)
            lua.push(entity.visionViewer, Lua.Conversion.NONE)
            return 1
        }

        /**
         * Gets a reference to this entity for storage and later retrieval.
         *
         * ```signatures
         * Ref() -> LuaReference
         * ```
         */
        private fun luaRef(lua: Lua): Int {
            val entity = lua.checkUserdata<Entity>(1)
            lua.push(entity.luaReference(), Lua.Conversion.NONE)
            return 1
        }

        /**
         * Sets the entity's coordinate and teleports it to the new position.
         *
         * ```signatures
         * SetCoordinate(coordinate: Coordinate)
         * ```
         */
        private fun luaSetCoordinate(lua: Lua): Int {
            val entity = lua.checkUserdata<Entity>(1)
            val (coordinate, _) = lua.checkCoordinate(2)
            entity.coordinate = coordinate
            entity.dimension?.syncManager?.entityTeleported(entity)
            return 0
        }

        /**
         * Spawns the entity in a dimension, making it visible to players.
         *
         * ```signatures
         * Spawn()
         * Spawn(dimension: Dimension)
         * ```
         */
        private fun luaSpawn(lua: Lua): Int {
            val entity = lua.checkUserdata<Entity>(1)
            val dimension = lua.toUserdata<Dimension>(2) ?: entity.world.dimensionManager.getOrCreateDimension(0)
            entity.dimension = dimension
            dimension.syncManager.entityAdded(entity)
            return 0
        }

        /**
         * Despawns the entity, removing it from its dimension.
         *
         * ```signatures
         * Despawn()
         * ```
         */
        private fun luaDespawn(lua: Lua): Int {
            val entity = lua.checkUserdata<Entity>(1)
            entity.despawn()
            return 0
        }

        /**
         * Permanently removes the entity from the world.
         *
         * ```signatures
         * Remove()
         * ```
         */
        private fun luaRemove(lua: Lua): Int {
            val entity = lua.checkUserdata<Entity>(1)
            entity.remove()
            return 0
        }

        /**
         * Sets the entity's facing direction.
         *
         * ```signatures
         * SetFacing(direction: Direction)
         * ```
         */
        private fun luaSetFacing(lua: Lua): Int {
            val entity = lua.checkUserdata<Entity>(1)
            val direction = lua.checkDirection(2, entity.world.grid)
            entity.facing = direction
            entity.dimension?.syncManager?.entityTurned(entity, direction)
            return 0
        }

        /**
         * Moves the entity one step in the specified direction.
         * Returns true if the move was successful, false if blocked.
         *
         * ```signatures
         * Move(direction: Direction) -> boolean
         * ```
         */
        private fun luaMove(lua: Lua): Int {
            val entity = lua.checkUserdata<Entity>(1)
            val direction = lua.checkDirection(2, entity.world.grid)
            val coordinate = entity.coordinate + direction.vector
            lua.push(entity.moveTo(coordinate))
            return 1
        }

        /**
         * Sets whether the entity can see objects with a specific vision tag.
         *
         * ```signatures
         * SetVision(enabled: boolean)
         * SetVision(enabled: boolean, tag: string)
         * ```
         */
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

        /**
         * Checks if the entity has vision for a specific tag.
         *
         * ```signatures
         * HasVision() -> boolean
         * HasVision(tag: string) -> boolean
         * ```
         */
        private fun luaHasVision(lua: Lua): Int {
            val entity = lua.checkUserdata<Entity>(1)
            val tagName = if (lua.isString(2)) lua.checkString(2) else "default"
            lua.push(entity.visionTags.contains(tagName))
            return 1
        }

        /**
         * Grants the entity vision for a specific tag.
         *
         * ```signatures
         * GrantVision()
         * GrantVision(tag: string)
         * ```
         */
        private fun luaGrantVision(lua: Lua): Int {
            val entity = lua.checkUserdata<Entity>(1)
            val tagName = if (lua.isString(2)) lua.checkString(2) else "default"
            entity.visionTags.add(tagName)
            return 0
        }

        /**
         * Revokes the entity's vision for a specific tag.
         *
         * ```signatures
         * RevokeVision()
         * RevokeVision(tag: string)
         * ```
         */
        private fun luaRevokeVision(lua: Lua): Int {
            val entity = lua.checkUserdata<Entity>(1)
            val tagName = if (lua.isString(2)) lua.checkString(2) else "default"
            entity.visionTags.remove(tagName)
            return 0
        }

        /**
         * Sets whether the entity is visible to objects with a specific vision tag.
         *
         * ```signatures
         * SetVisibility(enabled: boolean)
         * SetVisibility(enabled: boolean, tag: string)
         * ```
         */
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

        /**
         * Checks if the entity is visible for a specific tag.
         *
         * ```signatures
         * IsVisible() -> boolean
         * IsVisible(tag: string) -> boolean
         * ```
         */
        private fun luaIsVisible(lua: Lua): Int {
            val entity = lua.checkUserdata<Entity>(1)
            val tagName = if (lua.isString(2)) lua.checkString(2) else "default"
            lua.push(entity.visibilityTags.contains(tagName))
            return 1
        }

        /**
         * Checks if the entity is invisible for a specific tag.
         *
         * ```signatures
         * IsInvisible() -> boolean
         * IsInvisible(tag: string) -> boolean
         * ```
         */
        private fun luaIsInvisible(lua: Lua): Int {
            val entity = lua.checkUserdata<Entity>(1)
            val tagName = if (lua.isString(2)) lua.checkString(2) else "default"
            lua.push(!entity.visibilityTags.contains(tagName))
            return 1
        }

        /**
         * Makes the entity visible for a specific tag.
         *
         * ```signatures
         * MakeVisible()
         * MakeVisible(tag: string)
         * ```
         */
        private fun luaMakeVisible(lua: Lua): Int {
            val entity = lua.checkUserdata<Entity>(1)
            val tagName = if (lua.isString(2)) lua.checkString(2) else "default"
            entity.visibilityTags.add(tagName)
            return 0
        }

        /**
         * Makes the entity invisible for a specific tag.
         *
         * ```signatures
         * MakeInvisible()
         * MakeInvisible(tag: string)
         * ```
         */
        private fun luaMakeInvisible(lua: Lua): Int {
            val entity = lua.checkUserdata<Entity>(1)
            val tagName = if (lua.isString(2)) lua.checkString(2) else "default"
            entity.visibilityTags.remove(tagName)
            return 0
        }

        /**
         * Checks if the entity has collision enabled for a specific tag.
         *
         * ```signatures
         * HasCollisions() -> boolean
         * HasCollisions(tag: string) -> boolean
         * ```
         */
        private fun luaHasCollisions(lua: Lua): Int {
            val entity = lua.checkUserdata<Entity>(1)
            val tagName = if (lua.isString(2)) lua.checkString(2) else "default"
            lua.push(entity.collisionTags.contains(tagName))
            return 1
        }

        /**
         * Sets whether the entity has collision for a specific tag.
         *
         * ```signatures
         * SetCollisions(enabled: boolean)
         * SetCollisions(enabled: boolean, tag: string)
         * ```
         */
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

        /**
         * Enables collision for the entity with a specific tag.
         *
         * ```signatures
         * EnableCollisions()
         * EnableCollisions(tag: string)
         * ```
         */
        private fun luaEnableCollisions(lua: Lua): Int {
            val entity = lua.checkUserdata<Entity>(1)
            val tagName = if (lua.isString(2)) lua.checkString(2) else "default"
            entity.collisionTags.add(tagName)
            return 0
        }

        /**
         * Disables collision for the entity with a specific tag.
         *
         * ```signatures
         * DisableCollisions()
         * DisableCollisions(tag: string)
         * ```
         */
        private fun luaDisableCollisions(lua: Lua): Int {
            val entity = lua.checkUserdata<Entity>(1)
            val tagName = if (lua.isString(2)) lua.checkString(2) else "default"
            entity.collisionTags.remove(tagName)
            return 0
        }

        /**
         * Adds a dynamic component that is resolved per-player using a callback function.
         *
         * ```signatures
         * AddDynamicComponent(name: string, callback: function)
         * ```
         */
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

        /**
         * Gets all players that are currently controlling this entity.
         *
         * ```signatures
         * GetControllingPlayers() -> table[Player]
         * ```
         */
        private fun luaGetControllingPlayers(lua: Lua): Int {
            val entity = lua.checkUserdata<Entity>(1)
            lua.push(entity.getControllingPlayers(), Lua.Conversion.FULL)
            return 1
        }

        /**
         * Gets an attribute by name from this entity.
         *
         * ```signatures
         * GetAttribute(name: string) -> Attribute|nil
         * ```
         */
        private fun luaGetAttribute(lua: Lua): Int {
            val entity = lua.checkUserdata<Entity>(1)
            val name = lua.checkString(2)
            val attribute = entity.attributes[name]
            lua.push(attribute, Lua.Conversion.NONE)
            return 1
        }

        /**
         * Creates a new attribute on this entity with an initial value.
         *
         * ```signatures
         * CreateAttribute(name: string, initialValue: any) -> Attribute
         * ```
         */
        private fun luaCreateAttribute(lua: Lua): Int {
            val entity = lua.checkUserdata<Entity>(1)
            val name = lua.checkString(2)
            val initialValue = lua.toAny(3)
            val attribute = Attribute(entity, name, initialValue)
            entity.attributes[name] = attribute
            lua.push(attribute, Lua.Conversion.NONE)
            return 1
        }

        /**
         * Checks if the entity has a specific tag in its definition.
         *
         * ```signatures
         * HasTag(tag: string) -> boolean
         * ```
         */
        private fun luaHasTag(lua: Lua): Int {
            val entity = lua.checkUserdata<Entity>(1)
            val tag = lua.checkString(2)
            lua.push(entity.entityDefinition.tags.contains(tag))
            return 1
        }

        /**
         * Plays an animation on this entity for all watching players.
         *
         * ```signatures
         * PlayAnimation(animationName: string)
         * ```
         */
        private fun luaPlayAnimation(lua: Lua): Int {
            val entity = lua.checkUserdata<Entity>(1)
            val animationName = lua.checkString(2)
            entity.playAnimation(animationName)
            return 0
        }

        val luaMeta = LuaMappedMetatable(Entity::class) {
            getter(::luaGetNetworkId)
            getter(::luaGetCustomData)
            getter(::luaGetEntityDefinition)
            getter(::luaGetName)
            setter(::luaSetName)
            getter(::luaGetCoordinate)
            getter(::luaGetFacing)
            getter(::luaGetDimension)
            getter(::luaGetMap)
            getter(::luaGetCollisionViewer, "Collision")
            getter(::luaGetVisionViewer, "Vision")
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
