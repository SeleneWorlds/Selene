package world.selene.server.entities

import party.iroiro.luajava.Lua
import world.selene.common.data.ComponentConfiguration
import world.selene.common.data.EntityDefinition
import world.selene.common.grid.Grid
import world.selene.common.lua.LuaMappedMetatable
import world.selene.common.lua.LuaMetatable
import world.selene.common.lua.LuaMetatableProvider
import world.selene.common.lua.LuaReferencable
import world.selene.common.lua.LuaReference
import world.selene.common.lua.ManagedLuaTable
import world.selene.common.lua.checkBoolean
import world.selene.common.lua.checkCoordinate
import world.selene.common.lua.checkDirection
import world.selene.common.lua.checkFunction
import world.selene.common.lua.checkString
import world.selene.common.lua.toAny
import world.selene.common.lua.toAnyMap
import world.selene.common.lua.toFunction
import world.selene.common.lua.toUserdata
import world.selene.common.network.packet.EntityAnimationPacket
import world.selene.common.network.packet.TurnEntityPacket
import world.selene.common.util.Coordinate
import world.selene.server.attribute.Attribute
import world.selene.server.attribute.AttributeView
import world.selene.server.attribute.LuaAttributeViewObserver
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
    val attributeViews = mutableMapOf<String, AttributeView>()

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
            callable(Entity::despawn)
            callable(Entity::remove)
            callable("Ref") {
                it.push(it.checkSelf().luaReference(), Lua.Conversion.NONE)
                1
            }
            callable("SetCoordinate") {
                val entity = it.checkSelf()
                val (coordinate, _) = it.checkCoordinate(2)
                entity.coordinate = coordinate
                entity.dimension?.syncManager?.entityTeleported(entity)
                0
            }
            callable("SetFacing") {
                val entity = it.checkSelf()
                val direction = it.checkDirection(2, entity.world.grid)
                entity.facing = direction
                entity.dimension?.syncManager?.entityTurned(
                    entity,
                    direction
                )
                0
            }
            callable("Move") {
                val entity = it.checkSelf()
                val direction = it.checkDirection(2, entity.world.grid)
                val coordinate = entity.coordinate + direction.vector
                if (entity.moveTo(coordinate)) 0 else 1
            }
            callable("CollisionMap") {
                val entity = it.checkSelf()
                val (coordinate, _) = it.checkCoordinate(2)
                val chunkView = entity.world.chunkViewManager.atCoordinate(
                    entity.dimension!!,
                    entity.collisionViewer,
                    coordinate
                )
                it.push(chunkView, Lua.Conversion.NONE)
                1
            }
            callable("SetVision") {
                val entity = it.checkSelf()
                val enabled = it.checkBoolean(2)
                val tagName = if (it.isString(3)) it.checkString(3) else "default"
                if (enabled) {
                    entity.visionTags.add(tagName)
                } else {
                    entity.visionTags.remove(tagName)
                }
                0
            }
            callable("HasVision") {
                val entity = it.checkSelf()
                val tagName = if (it.isString(2)) it.checkString(2) else "default"
                it.push(entity.visionTags.contains(tagName))
                1
            }
            callable("GrantVision") {
                val entity = it.checkSelf()
                val tagName = if (it.isString(2)) it.checkString(2) else "default"
                entity.visionTags.add(tagName)
                0
            }
            callable("RevokeVision") {
                val entity = it.checkSelf()
                val tagName = if (it.isString(2)) it.checkString(2) else "default"
                entity.visionTags.remove(tagName)
                0
            }
            callable("SetVisibility") {
                val entity = it.checkSelf()
                val enabled = it.checkBoolean(2)
                val tagName = if (it.isString(3)) it.checkString(3) else "default"
                if (enabled) {
                    entity.visibilityTags.add(tagName)
                } else {
                    entity.visibilityTags.remove(tagName)
                }
                0
            }
            callable("IsVisible") {
                val entity = it.checkSelf()
                val tagName = if (it.isString(2)) it.checkString(2) else "default"
                it.push(entity.visibilityTags.contains(tagName))
                1
            }
            callable("IsInvisible") {
                val entity = it.checkSelf()
                val tagName = if (it.isString(2)) it.checkString(2) else "default"
                it.push(!entity.visibilityTags.contains(tagName))
                1
            }
            callable("MakeVisible") {
                val entity = it.checkSelf()
                val tagName = if (it.isString(2)) it.checkString(2) else "default"
                entity.visibilityTags.add(tagName)
                0
            }
            callable("MakeInvisible") {
                val entity = it.checkSelf()
                val tagName = if (it.isString(2)) it.checkString(2) else "default"
                entity.visibilityTags.remove(tagName)
                0
            }
            callable("HasCollisions") {
                val entity = it.checkSelf()
                val tagName = if (it.isString(2)) it.checkString(2) else "default"
                it.push(entity.collisionTags.contains(tagName))
                1
            }
            callable("SetCollisions") {
                val entity = it.checkSelf()
                val enabled = it.checkBoolean(2)
                val tagName = if (it.isString(3)) it.checkString(3) else "default"
                if (enabled) {
                    entity.collisionTags.add(tagName)
                } else {
                    entity.collisionTags.remove(tagName)
                }
                0
            }
            callable("EnableCollisions") {
                val entity = it.checkSelf()
                val tagName = if (it.isString(2)) it.checkString(2) else "default"
                entity.collisionTags.add(tagName)
                0
            }
            callable("DisableCollisions") {
                val entity = it.checkSelf()
                val tagName = if (it.isString(2)) it.checkString(2) else "default"
                entity.collisionTags.remove(tagName)
                0
            }
            callable("Spawn") {
                val entity = it.checkSelf()
                val dimension = it.toUserdata<Dimension>(2) ?: entity.world.dimensionManager.getOrCreateDimension(0)
                entity.dimension = dimension
                dimension.syncManager.entityAdded(entity)
                0
            }
            callable("AddDynamicComponent") {
                val entity = it.checkSelf()
                val name = it.checkString(2)
                it.pushValue(3)
                val callback = it.get()
                entity.dynamicComponents[name] = object : ComponentResolver {
                    override fun resolveForPlayer(player: Player): ComponentConfiguration? {
                        val lua = callback.state()
                        lua.push(callback)
                        lua.push(entity, Lua.Conversion.NONE)
                        lua.push(player, Lua.Conversion.NONE)
                        lua.pCall(2, 1)
                        return objectMapper.convertValue(lua.toAnyMap(-1), ComponentConfiguration::class.java)
                    }
                }
                0
            }
            callable("GetControllingPlayers") {
                val entity = it.checkSelf()
                it.push(entity.getControllingPlayers(), Lua.Conversion.FULL)
                1
            }
            callable("GetAttribute") {
                val entity = it.checkSelf()
                val name = it.checkString(2)
                val attribute = entity.attributes[name]
                it.push(attribute, Lua.Conversion.NONE)
                1
            }
            callable("CreateAttribute") {
                val entity = it.checkSelf()
                val name = it.checkString(2)
                val initialValue = it.toAny(3)
                val attribute = Attribute(entity, name, initialValue)
                entity.attributes[name] = attribute
                it.push(attribute, Lua.Conversion.NONE)
                1
            }
            callable("GetOrCreateAttribute") {
                val entity = it.checkSelf()
                val name = it.checkString(2)
                val initialValue = it.toAny(3)
                val initializer = it.toFunction(4)
                val attribute = entity.attributes.getOrPut(name) {
                    Attribute(entity, name, initialValue)
                }
                if (initializer != null) {
                    it.push(initializer)
                    it.push(attribute, Lua.Conversion.NONE)
                    it.pCall(1, 0)
                }
                it.push(attribute, Lua.Conversion.NONE)
                1
            }
            callable("GetAttributeView") { lua ->
                val entity = lua.checkSelf()
                val name = lua.checkString(2)
                val view = entity.attributeViews[name]
                if (view != null) {
                    lua.push(view, Lua.Conversion.NONE)
                    return@callable 1
                }
                0
            }
            callable("CreateAttributeView") { lua ->
                val entity = lua.checkSelf()
                val name = lua.checkString(2)
                if (entity.attributeViews.containsKey(name)) {
                    return@callable lua.error(IllegalArgumentException("Attribute view $name already exists"))
                }
                val callback = lua.checkFunction(3)
                val view = AttributeView(entity, name, LuaAttributeViewObserver(callback))
                entity.attributeViews[name] = view
                lua.push(view, Lua.Conversion.NONE)
                1
            }
            callable("DestroyAttributeView") { lua ->
                val entity = lua.checkSelf()
                val name = lua.checkString(2)
                val view = entity.attributeViews.remove(name)
                view?.dispose()
                0
            }
            callable("HasTag") { lua ->
                val entity = lua.checkSelf()
                val tag = lua.checkString(2)
                lua.push(entity.entityDefinition.tags.contains(tag))
                1
            }
            callable("PlayAnimation") { lua ->
                val entity = lua.checkSelf()
                val animationName = lua.checkString(2)
                entity.playAnimation(animationName)
                0
            }
        }
    }

}
