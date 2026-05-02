package com.seleneworlds.server.entities

import com.seleneworlds.common.entities.ComponentConfiguration
import com.seleneworlds.common.lua.LuaMappedMetatable
import com.seleneworlds.common.lua.util.*
import com.seleneworlds.common.script.ConstantTrace
import com.seleneworlds.common.serialization.seleneJson
import com.seleneworlds.common.serialization.toJsonElement
import com.seleneworlds.server.dimensions.DimensionApi
import party.iroiro.luajava.Lua

object EntityLuaApi {

    private fun luaGetNetworkId(lua: Lua): Int {
        val entity = lua.checkUserdata<EntityApi>(1)
        lua.push(entity.getNetworkId())
        return 1
    }

    private fun luaGetCustomData(lua: Lua): Int {
        val entity = lua.checkUserdata<EntityApi>(1)
        lua.push(entity.getCustomData(), Lua.Conversion.NONE)
        return 1
    }

    private fun luaGetEntityDefinition(lua: Lua): Int {
        val entity = lua.checkUserdata<EntityApi>(1)
        lua.push(entity.getEntityDefinition().get(), Lua.Conversion.NONE)
        return 1
    }

    private fun luaGetName(lua: Lua): Int {
        val entity = lua.checkUserdata<EntityApi>(1)
        lua.push(entity.getName())
        return 1
    }

    private fun luaSetName(lua: Lua): Int {
        val entity = lua.checkUserdata<EntityApi>(1)
        entity.setName(lua.checkString(3))
        return 0
    }

    private fun luaGetCoordinate(lua: Lua): Int {
        val entity = lua.checkUserdata<EntityApi>(1)
        lua.push(entity.getCoordinate(), Lua.Conversion.NONE)
        return 1
    }

    private fun luaGetFacing(lua: Lua): Int {
        val entity = lua.checkUserdata<EntityApi>(1)
        lua.push(entity.getFacing(), Lua.Conversion.NONE)
        return 1
    }

    private fun luaGetDimension(lua: Lua): Int {
        val entity = lua.checkUserdata<EntityApi>(1)
        lua.push(entity.getDimension(), Lua.Conversion.NONE)
        return 1
    }

    private fun luaGetMap(lua: Lua): Int {
        val entity = lua.checkUserdata<EntityApi>(1)
        lua.push(entity.getMap(), Lua.Conversion.NONE)
        return 1
    }

    private fun luaGetCollisionViewer(lua: Lua): Int {
        val entity = lua.checkUserdata<EntityApi>(1)
        lua.push(entity.getCollisionViewer(), Lua.Conversion.NONE)
        return 1
    }

    private fun luaGetVisionViewer(lua: Lua): Int {
        val entity = lua.checkUserdata<EntityApi>(1)
        lua.push(entity.getVisionViewer(), Lua.Conversion.NONE)
        return 1
    }

    private fun luaRef(lua: Lua): Int {
        val entity = lua.checkUserdata<EntityApi>(1)
        lua.push(entity.entity.resolvableReference(), Lua.Conversion.NONE)
        return 1
    }

    private fun luaSetCoordinate(lua: Lua): Int {
        val entity = lua.checkUserdata<EntityApi>(1)
        val (coordinate, _) = lua.checkCoordinate(2)
        entity.setCoordinate(coordinate)
        return 0
    }

    private fun luaSpawn(lua: Lua): Int {
        val entity = lua.checkUserdata<EntityApi>(1)
        val dimension = lua.toUserdata<DimensionApi>(2)?.dimension
        entity.spawn(dimension)
        return 0
    }

    private fun luaDespawn(lua: Lua): Int {
        val entity = lua.checkUserdata<EntityApi>(1)
        entity.despawn()
        return 0
    }

    private fun luaRemove(lua: Lua): Int {
        val entity = lua.checkUserdata<EntityApi>(1)
        entity.remove()
        return 0
    }

    private fun luaSetFacing(lua: Lua): Int {
        val entity = lua.checkUserdata<EntityApi>(1)
        entity.setFacing(lua.checkDirection(2, entity.entity.world.grid))
        return 0
    }

    private fun luaMove(lua: Lua): Int {
        val entity = lua.checkUserdata<EntityApi>(1)
        lua.push(entity.move(lua.checkDirection(2, entity.entity.world.grid)))
        return 1
    }

    private fun luaSetVision(lua: Lua): Int {
        val entity = lua.checkUserdata<EntityApi>(1)
        val enabled = lua.checkBoolean(2)
        val tagName = if (lua.isString(3)) lua.checkString(3) else "default"
        entity.setVision(enabled, tagName)
        return 0
    }

    private fun luaHasVision(lua: Lua): Int {
        val entity = lua.checkUserdata<EntityApi>(1)
        val tagName = if (lua.isString(2)) lua.checkString(2) else "default"
        lua.push(entity.hasVision(tagName))
        return 1
    }

    private fun luaGrantVision(lua: Lua): Int {
        val entity = lua.checkUserdata<EntityApi>(1)
        val tagName = if (lua.isString(2)) lua.checkString(2) else "default"
        entity.grantVision(tagName)
        return 0
    }

    private fun luaRevokeVision(lua: Lua): Int {
        val entity = lua.checkUserdata<EntityApi>(1)
        val tagName = if (lua.isString(2)) lua.checkString(2) else "default"
        entity.revokeVision(tagName)
        return 0
    }

    private fun luaSetVisibility(lua: Lua): Int {
        val entity = lua.checkUserdata<EntityApi>(1)
        val enabled = lua.checkBoolean(2)
        val tagName = if (lua.isString(3)) lua.checkString(3) else "default"
        entity.setVisibility(enabled, tagName)
        return 0
    }

    private fun luaIsVisible(lua: Lua): Int {
        val entity = lua.checkUserdata<EntityApi>(1)
        val tagName = if (lua.isString(2)) lua.checkString(2) else "default"
        lua.push(entity.isVisible(tagName))
        return 1
    }

    private fun luaIsInvisible(lua: Lua): Int {
        val entity = lua.checkUserdata<EntityApi>(1)
        val tagName = if (lua.isString(2)) lua.checkString(2) else "default"
        lua.push(entity.isInvisible(tagName))
        return 1
    }

    private fun luaMakeVisible(lua: Lua): Int {
        val entity = lua.checkUserdata<EntityApi>(1)
        val tagName = if (lua.isString(2)) lua.checkString(2) else "default"
        entity.makeVisible(tagName)
        return 0
    }

    private fun luaMakeInvisible(lua: Lua): Int {
        val entity = lua.checkUserdata<EntityApi>(1)
        val tagName = if (lua.isString(2)) lua.checkString(2) else "default"
        entity.makeInvisible(tagName)
        return 0
    }

    private fun luaHasCollisions(lua: Lua): Int {
        val entity = lua.checkUserdata<EntityApi>(1)
        val tagName = if (lua.isString(2)) lua.checkString(2) else "default"
        lua.push(entity.hasCollisions(tagName))
        return 1
    }

    private fun luaSetCollisions(lua: Lua): Int {
        val entity = lua.checkUserdata<EntityApi>(1)
        val enabled = lua.checkBoolean(2)
        val tagName = if (lua.isString(3)) lua.checkString(3) else "default"
        entity.setCollisions(enabled, tagName)
        return 0
    }

    private fun luaEnableCollisions(lua: Lua): Int {
        val entity = lua.checkUserdata<EntityApi>(1)
        val tagName = if (lua.isString(2)) lua.checkString(2) else "default"
        entity.enableCollisions(tagName)
        return 0
    }

    private fun luaDisableCollisions(lua: Lua): Int {
        val entity = lua.checkUserdata<EntityApi>(1)
        val tagName = if (lua.isString(2)) lua.checkString(2) else "default"
        entity.disableCollisions(tagName)
        return 0
    }

    private fun luaAddDynamicComponent(lua: Lua): Int {
        val entity = lua.checkUserdata<EntityApi>(1)
        val name = lua.checkString(2)
        lua.pushValue(3)
        val callback = lua.get()
        val registrationSite = lua.getCallerInfo()
        entity.addDynamicComponent(name) { player ->
            val callbackLua = callback.state()
            callbackLua.push(callback)
            callbackLua.push(entity, Lua.Conversion.NONE)
            callbackLua.push(player.api, Lua.Conversion.NONE)
            callbackLua.xpCall(2, 1, ConstantTrace("[dynamic component \"$name\"] registered in $registrationSite"))
            try {
                seleneJson.decodeFromJsonElement(
                    ComponentConfiguration.serializer(),
                    (callbackLua.toSerializedMap(-1) ?: emptyMap()).toJsonElement()
                )
            } finally {
                callbackLua.pop(1)
            }
        }
        return 0
    }

    private fun luaGetControllingPlayers(lua: Lua): Int {
        val entity = lua.checkUserdata<EntityApi>(1)
        lua.push(entity.getControllingPlayers(), Lua.Conversion.FULL)
        return 1
    }

    private fun luaGetAttribute(lua: Lua): Int {
        val entity = lua.checkUserdata<EntityApi>(1)
        lua.push(entity.getAttribute(lua.checkString(2)), Lua.Conversion.NONE)
        return 1
    }

    private fun luaCreateAttribute(lua: Lua): Int {
        val entity = lua.checkUserdata<EntityApi>(1)
        val name = lua.checkString(2)
        val initialValue = lua.toAny(3)
        lua.push(entity.createAttribute(name, initialValue), Lua.Conversion.NONE)
        return 1
    }

    private fun luaHasTag(lua: Lua): Int {
        val entity = lua.checkUserdata<EntityApi>(1)
        lua.push(entity.hasTag(lua.checkString(2)))
        return 1
    }

    private fun luaPlayAnimation(lua: Lua): Int {
        val entity = lua.checkUserdata<EntityApi>(1)
        entity.playAnimation(lua.checkString(2))
        return 0
    }

    val luaMeta = LuaMappedMetatable(EntityApi::class) {
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
