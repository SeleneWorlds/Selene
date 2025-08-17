package world.selene.server.entities

import party.iroiro.luajava.Lua
import party.iroiro.luajava.value.LuaValue
import world.selene.common.data.ConfiguredComponent
import world.selene.common.grid.Grid
import world.selene.common.lua.checkBoolean
import world.selene.common.lua.checkDirection
import world.selene.common.lua.checkJavaObject
import world.selene.common.lua.checkString
import world.selene.common.util.Coordinate
import world.selene.server.lua.Scripting
import world.selene.server.world.World
import world.selene.server.cameras.Viewer
import world.selene.server.data.Registries
import world.selene.server.dimensions.Dimension
import world.selene.server.maps.MapLayer
import world.selene.server.objectMapper
import world.selene.server.player.Player

class Entity(val registries: Registries, val world: World, val scripting: Scripting) {
    var networkId: Int = -1
    var entityType: String = ""
    val name = "John Selene"
    var coordinate = Coordinate(0, 0, 0)
    var facing: Grid.Direction? = null
    var dimension: Dimension? = null
    val luaProxy = EntityLuaProxy(this)
    val customData = mutableMapOf<String, Any>()
    val dynamicComponents = mutableMapOf<String, ComponentResolver>()

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
        fun resolveForPlayer(player: Player): ConfiguredComponent?
    }

    val visibilityTags = mutableSetOf("default")
    val visionTags = mutableSetOf("default")
    val collisionTags = mutableSetOf("default")


    fun resolveComponentsFor(player: Player): Map<String, ConfiguredComponent> {
        val components = mutableMapOf<String, ConfiguredComponent>()
        dynamicComponents.forEach { (key, value) ->
            value.resolveForPlayer(player)?.let {
                components[key] = it
            }
        }
        return components
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
        scripting.signals.entitySteppedOnTile.emit {
            it.push(luaProxy, Lua.Conversion.NONE)
            it.push(coordinate, Lua.Conversion.NONE)
            2
        }
        return true
    }

    class EntityLuaProxy(val delegate: Entity) {
        val Name get() = delegate.name
        val Coordinate get() = delegate.coordinate
        val Facing get() = delegate.facing
        val Dimension get() = delegate.dimension?.luaProxy
        val Map get() = delegate.dimension?.mapTree?.luaProxy
        val Collision get() = delegate.collisionViewer
        val Vision get() = delegate.visionViewer

        fun SetCoordinate(x: Int, y: Int, z: Int) {
            delegate.coordinate = Coordinate(x, y, z)
            delegate.dimension?.syncManager?.entityTeleported(delegate)
        }

        fun SetFacing(lua: Lua): Int {
            val direction = lua.checkDirection(2, delegate.world.grid)
            delegate.facing = direction
            delegate.dimension?.syncManager?.entityTurned(
                delegate,
                direction
            )
            return 0
        }

        fun GetCustomData(lua: Lua): Int {
            val key = lua.checkString(2)
            val defaultValue = lua.toObject(3)
            val value = delegate.customData.getOrDefault(key, defaultValue)
            lua.push(value, Lua.Conversion.FULL)
            return 1
        }

        fun SetCustomData(lua: Lua): Int {
            val key = lua.checkString(2)
            val value = lua.toObject(3)!!
            delegate.customData[key] = value
            return 0
        }

        fun CollisionMap(lua: Lua): Int {
            val coordinate = lua.checkJavaObject<Coordinate>(2)
            val chunkView = delegate.world.chunkViewManager.atCoordinate(
                delegate.dimension!!,
                delegate.collisionViewer,
                coordinate
            ).luaProxy
            lua.push(chunkView, Lua.Conversion.NONE)
            return 1
        }

        fun SetVision(lua: Lua): Int {
            val enabled = lua.checkBoolean(2)
            val tagName = if (lua.isString(3)) lua.checkString(3) else "default"
            if (enabled) {
                delegate.visionTags.add(tagName)
            } else {
                delegate.visionTags.remove(tagName)
            }
            return 0
        }

        fun HasVision(lua: Lua): Int {
            val tagName = if (lua.isString(2)) lua.checkString(2) else "default"
            lua.push(delegate.visionTags.contains(tagName))
            return 0
        }

        fun GrantVision(lua: Lua): Int {
            val tagName = if (lua.isString(2)) lua.checkString(2) else "default"
            delegate.visionTags.add(tagName)
            return 0
        }

        fun RevokeVision(lua: Lua): Int {
            val tagName = if (lua.isString(2)) lua.checkString(2) else "default"
            delegate.visionTags.remove(tagName)
            return 0
        }

        fun SetVisibility(lua: Lua): Int {
            val enabled = lua.checkBoolean(2)
            val tagName = if (lua.isString(3)) lua.checkString(3) else "default"
            if (enabled) {
                delegate.visibilityTags.add(tagName)
            } else {
                delegate.visibilityTags.remove(tagName)
            }
            return 0
        }

        fun IsVisible(lua: Lua): Int {
            val tagName = if (lua.isString(2)) lua.checkString(2) else "default"
            lua.push(delegate.visibilityTags.contains(tagName))
            return 1
        }

        fun IsInvisible(lua: Lua): Int {
            val tagName = if (lua.isString(2)) lua.checkString(2) else "default"
            lua.push(!delegate.visibilityTags.contains(tagName))
            return 1
        }

        fun MakeVisible(lua: Lua): Int {
            val tagName = if (lua.isString(2)) lua.checkString(2) else "default"
            delegate.visibilityTags.add(tagName)
            return 0
        }

        fun MakeInvisible(lua: Lua): Int {
            val tagName = if (lua.isString(2)) lua.checkString(2) else "default"
            delegate.visibilityTags.remove(tagName)
            return 0
        }

        fun HasCollisions(lua: Lua): Int {
            val tagName = if (lua.isString(2)) lua.checkString(2) else "default"
            lua.push(delegate.collisionTags.contains(tagName))
            return 0
        }

        fun SetCollisions(lua: Lua): Int {
            val enabled = lua.checkBoolean(2)
            val tagName = if (lua.isString(3)) lua.checkString(3) else "default"
            if (enabled) {
                delegate.collisionTags.add(tagName)
            } else {
                delegate.collisionTags.remove(tagName)
            }
            return 0
        }

        fun EnableCollisions(lua: Lua): Int {
            val tagName = if (lua.isString(2)) lua.checkString(2) else "default"
            delegate.collisionTags.add(tagName)
            return 0
        }

        fun DisableCollisions(lua: Lua): Int {
            val tagName = if (lua.isString(2)) lua.checkString(2) else "default"
            delegate.collisionTags.remove(tagName)
            return 0
        }

        fun Spawn(lua: Lua): Int {
            val dimension =
                if (lua.isUserdata(2)) lua.checkJavaObject<Dimension.DimensionLuaProxy>(2).delegate else delegate.world.dimensionManager.getOrCreateDimension(
                    0
                )
            delegate.dimension = dimension
            return 0
        }

        fun Despawn() {
            delegate.dimension = null
        }

        fun Remove() {
            delegate.world.entityManager.removeEntity(delegate)
        }

        fun AddDynamicComponent(name: String, callback: LuaValue) {
            delegate.dynamicComponents[name] = object : ComponentResolver {
                override fun resolveForPlayer(player: Player): ConfiguredComponent? {
                    val lua = callback.state()
                    lua.push(callback)
                    lua.push(this@EntityLuaProxy, Lua.Conversion.NONE)
                    lua.push(player.luaProxy, Lua.Conversion.NONE)
                    lua.pCall(2, 1)
                    return objectMapper.convertValue(lua.toMap(-1), ConfiguredComponent::class.java)
                }
            }
        }
    }
}
