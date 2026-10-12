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
    private fun isActive(lua: Lua): Int {
        lua.push(lua.checkUserdata<EntityApi>(1).isActive())
        return 1
    }
    private fun isAwake(lua: Lua): Int {
        lua.push(lua.checkUserdata<EntityApi>(1).isAwake())
        return 1
    }
    private fun isEnabled(lua: Lua): Int {
        lua.push(lua.checkUserdata<EntityApi>(1).isEnabled())
        return 1
    }
    private fun setEnabled(lua: Lua): Int {
        lua.checkUserdata<EntityApi>(1).setEnabled(lua.checkBoolean(2))
        return 0
    }
    private fun setKeepAwake(lua: Lua): Int {
        lua.checkUserdata<EntityApi>(1).setKeepAwake(lua.checkBoolean(2))
        return 0
    }
    /** Opt-in proximity sleeping; defaults: wake 60, sleep 70, vertical range 2. */
    private fun setSleepWhenUnobserved(lua: Lua): Int {
        lua.checkUserdata<EntityApi>(1).setSleepWhenUnobserved(lua.checkBoolean(2),
            if (lua.top >= 3) lua.checkInt(3) else 60,
            if (lua.top >= 4) lua.checkInt(4) else 70,
            if (lua.top >= 5) lua.checkInt(5) else 2)
        return 0
    }

    private fun isMoving(lua: Lua): Int {
        val entity = lua.checkUserdata<EntityApi>(1)
        lua.push(entity.isMoving())
        return 1
    }

    private fun getMovementDurationMultiplier(lua: Lua): Int {
        val entity = lua.checkUserdata<EntityApi>(1)
        lua.push(entity.getMovementDurationMultiplier())
        return 1
    }

    private fun setMovementDurationMultiplier(lua: Lua): Int {
        val entity = lua.checkUserdata<EntityApi>(1)
        entity.setMovementDurationMultiplier(lua.checkFloat(2))
        return 0
    }

    private fun getNetworkId(lua: Lua): Int {
        val entity = lua.checkUserdata<EntityApi>(1)
        lua.push(entity.getNetworkId())
        return 1
    }

    private fun getRuntimeData(lua: Lua): Int {
        val api = lua.checkUserdata<EntityApi>(1)
        val namespace = lua.checkString(2)
        val customLuaData = api.delegate.customLuaData
            ?: lua.newTable().let { lua.get() }.also { api.delegate.customLuaData = it }

        customLuaData.push(lua)
        lua.push(namespace)
        lua.rawGet(-2)

        if (lua.isNil(-1)) {
            lua.pop(1) // pop the nil
            lua.newTable() // new table for this namespace
            lua.push(namespace)
            lua.pushValue(-2) // copy the new table
            lua.rawSet(-4)
        }
        lua.remove(-2)
        return 1
    }

    private fun overwriteRuntimeData(lua: Lua): Int {
        val api = lua.checkUserdata<EntityApi>(1)
        val namespace = lua.checkString(2)
        lua.checkType(3, Lua.LuaType.TABLE)
        val customLuaData = api.delegate.customLuaData
            ?: lua.newTable().let { lua.get() }.also { api.delegate.customLuaData = it }

        customLuaData.push(lua)
        lua.push(namespace)
        lua.pushValue(3)
        lua.rawSet(-3)
        return 0
    }

    private fun hasRuntimeData(lua: Lua): Int {
        val api = lua.checkUserdata<EntityApi>(1)
        val namespace = lua.checkString(2)
        val customLuaData = api.delegate.customLuaData

        if (customLuaData == null) {
            lua.push(false)
            return 1
        }

        customLuaData.push(lua)
        lua.push(namespace)
        lua.rawGet(-2)
        lua.push(!lua.isNil(-1))
        return 1
    }

    private fun removeRuntimeData(lua: Lua): Int {
        val api = lua.checkUserdata<EntityApi>(1)
        val namespace = lua.checkString(2)
        val customLuaData = api.delegate.customLuaData ?: return 0

        customLuaData.push(lua)
        lua.push(namespace)
        lua.pushNil()
        lua.rawSet(-3)
        return 0
    }

    private fun getEntityDefinition(lua: Lua): Int {
        val entity = lua.checkUserdata<EntityApi>(1)
        lua.push(entity.getEntityDefinition().get(), Lua.Conversion.NONE)
        return 1
    }

    private fun getName(lua: Lua): Int {
        val entity = lua.checkUserdata<EntityApi>(1)
        lua.push(entity.getName())
        return 1
    }

    private fun setName(lua: Lua): Int {
        val entity = lua.checkUserdata<EntityApi>(1)
        entity.setName(lua.checkString(2))
        return 0
    }

    private fun getCoordinate(lua: Lua): Int {
        val entity = lua.checkUserdata<EntityApi>(1)
        lua.push(entity.getCoordinate(), Lua.Conversion.NONE)
        return 1
    }

    private fun getFacing(lua: Lua): Int {
        val entity = lua.checkUserdata<EntityApi>(1)
        lua.push(entity.getFacing(), Lua.Conversion.NONE)
        return 1
    }

    private fun getDimension(lua: Lua): Int {
        val entity = lua.checkUserdata<EntityApi>(1)
        lua.push(entity.getDimension(), Lua.Conversion.NONE)
        return 1
    }

    private fun getMap(lua: Lua): Int {
        val entity = lua.checkUserdata<EntityApi>(1)
        lua.push(entity.getMap(), Lua.Conversion.NONE)
        return 1
    }

    private fun getCollisionViewer(lua: Lua): Int {
        val entity = lua.checkUserdata<EntityApi>(1)
        lua.push(entity.getCollisionViewer(), Lua.Conversion.NONE)
        return 1
    }

    private fun getVisionViewer(lua: Lua): Int {
        val entity = lua.checkUserdata<EntityApi>(1)
        lua.push(entity.getVisionViewer(), Lua.Conversion.NONE)
        return 1
    }

    private fun getInteractionViewer(lua: Lua): Int {
        val entity = lua.checkUserdata<EntityApi>(1)
        lua.push(entity.getInteractionViewer(), Lua.Conversion.NONE)
        return 1
    }

    private fun ref(lua: Lua): Int {
        val entity = lua.checkUserdata<EntityApi>(1)
        lua.push(entity.delegate.resolvableReference(), Lua.Conversion.NONE)
        return 1
    }

    private fun setCoordinate(lua: Lua): Int {
        val entity = lua.checkUserdata<EntityApi>(1)
        val (coordinate, _) = lua.checkCoordinate(2)
        entity.setCoordinate(coordinate)
        return 0
    }

    private fun spawn(lua: Lua): Int {
        val entity = lua.checkUserdata<EntityApi>(1)
        val dimension = lua.toUserdata<DimensionApi>(2)?.dimension
        entity.spawn(dimension)
        return 0
    }

    private fun despawn(lua: Lua): Int {
        val entity = lua.checkUserdata<EntityApi>(1)
        entity.despawn()
        return 0
    }

    private fun remove(lua: Lua): Int {
        val entity = lua.checkUserdata<EntityApi>(1)
        entity.remove()
        return 0
    }

    private fun setFacing(lua: Lua): Int {
        val entity = lua.checkUserdata<EntityApi>(1)
        entity.setFacing(lua.checkDirection(2, entity.delegate.world.grid))
        return 0
    }

    private fun move(lua: Lua): Int {
        val entity = lua.checkUserdata<EntityApi>(1)
        lua.push(entity.move(lua.checkDirection(2, entity.delegate.world.grid)))
        return 1
    }

    private fun setVision(lua: Lua): Int {
        val entity = lua.checkUserdata<EntityApi>(1)
        val enabled = lua.checkBoolean(2)
        val tagName = if (lua.isString(3)) lua.checkString(3) else "default"
        entity.setVision(enabled, tagName)
        return 0
    }

    private fun hasVision(lua: Lua): Int {
        val entity = lua.checkUserdata<EntityApi>(1)
        val tagName = if (lua.isString(2)) lua.checkString(2) else "default"
        lua.push(entity.hasVision(tagName))
        return 1
    }

    private fun grantVision(lua: Lua): Int {
        val entity = lua.checkUserdata<EntityApi>(1)
        val tagName = if (lua.isString(2)) lua.checkString(2) else "default"
        entity.grantVision(tagName)
        return 0
    }

    private fun revokeVision(lua: Lua): Int {
        val entity = lua.checkUserdata<EntityApi>(1)
        val tagName = if (lua.isString(2)) lua.checkString(2) else "default"
        entity.revokeVision(tagName)
        return 0
    }

    private fun setVisibility(lua: Lua): Int {
        val entity = lua.checkUserdata<EntityApi>(1)
        val enabled = lua.checkBoolean(2)
        val tagName = if (lua.isString(3)) lua.checkString(3) else "default"
        entity.setVisibility(enabled, tagName)
        return 0
    }

    private fun isVisible(lua: Lua): Int {
        val entity = lua.checkUserdata<EntityApi>(1)
        val tagName = if (lua.isString(2)) lua.checkString(2) else "default"
        lua.push(entity.isVisible(tagName))
        return 1
    }

    private fun isInvisible(lua: Lua): Int {
        val entity = lua.checkUserdata<EntityApi>(1)
        val tagName = if (lua.isString(2)) lua.checkString(2) else "default"
        lua.push(entity.isInvisible(tagName))
        return 1
    }

    private fun makeVisible(lua: Lua): Int {
        val entity = lua.checkUserdata<EntityApi>(1)
        val tagName = if (lua.isString(2)) lua.checkString(2) else "default"
        entity.makeVisible(tagName)
        return 0
    }

    private fun makeInvisible(lua: Lua): Int {
        val entity = lua.checkUserdata<EntityApi>(1)
        val tagName = if (lua.isString(2)) lua.checkString(2) else "default"
        entity.makeInvisible(tagName)
        return 0
    }

    private fun hasCollisions(lua: Lua): Int {
        val entity = lua.checkUserdata<EntityApi>(1)
        val tagName = if (lua.isString(2)) lua.checkString(2) else "default"
        lua.push(entity.hasCollisions(tagName))
        return 1
    }

    private fun setCollisions(lua: Lua): Int {
        val entity = lua.checkUserdata<EntityApi>(1)
        val enabled = lua.checkBoolean(2)
        val tagName = if (lua.isString(3)) lua.checkString(3) else "default"
        entity.setCollisions(enabled, tagName)
        return 0
    }

    private fun enableCollisions(lua: Lua): Int {
        val entity = lua.checkUserdata<EntityApi>(1)
        val tagName = if (lua.isString(2)) lua.checkString(2) else "default"
        entity.enableCollisions(tagName)
        return 0
    }

    private fun disableCollisions(lua: Lua): Int {
        val entity = lua.checkUserdata<EntityApi>(1)
        val tagName = if (lua.isString(2)) lua.checkString(2) else "default"
        entity.disableCollisions(tagName)
        return 0
    }

    private fun setGravityEnabled(lua: Lua): Int {
        val entity = lua.checkUserdata<EntityApi>(1)
        entity.setGravityEnabled(lua.checkBoolean(2))
        return 0
    }

    private fun hasInteractions(lua: Lua): Int {
        val entity = lua.checkUserdata<EntityApi>(1)
        val tagName = if (lua.isString(2)) lua.checkString(2) else "default"
        lua.push(entity.hasInteractions(tagName))
        return 1
    }

    private fun setInteractions(lua: Lua): Int {
        val entity = lua.checkUserdata<EntityApi>(1)
        val enabled = lua.checkBoolean(2)
        val tagName = if (lua.isString(3)) lua.checkString(3) else "default"
        entity.setInteractions(enabled, tagName)
        return 0
    }

    private fun enableInteractions(lua: Lua): Int {
        val entity = lua.checkUserdata<EntityApi>(1)
        val tagName = if (lua.isString(2)) lua.checkString(2) else "default"
        entity.enableInteractions(tagName)
        return 0
    }

    private fun disableInteractions(lua: Lua): Int {
        val entity = lua.checkUserdata<EntityApi>(1)
        val tagName = if (lua.isString(2)) lua.checkString(2) else "default"
        entity.disableInteractions(tagName)
        return 0
    }

    private fun addDynamicComponent(lua: Lua): Int {
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
                if (callbackLua.isNil(-1)) {
                    return@addDynamicComponent null
                }
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

    private fun getPrimaryControllingPlayer(lua: Lua): Int {
        val entity = lua.checkUserdata<EntityApi>(1)
        lua.push(entity.getPrimaryControllingPlayer(), Lua.Conversion.NONE)
        return 1
    }

    private fun getControllingPlayers(lua: Lua): Int {
        val entity = lua.checkUserdata<EntityApi>(1)
        lua.push(entity.getControllingPlayers(), Lua.Conversion.FULL)
        return 1
    }

    private fun hasTag(lua: Lua): Int {
        val entity = lua.checkUserdata<EntityApi>(1)
        lua.push(entity.hasTag(lua.checkString(2)))
        return 1
    }

    private fun playAnimation(lua: Lua): Int {
        val entity = lua.checkUserdata<EntityApi>(1)
        entity.playAnimation(lua.checkString(2))
        return 0
    }

    private fun updateVisuals(lua: Lua): Int {
        val entity = lua.checkUserdata<EntityApi>(1)
        entity.updateVisuals()
        return 0
    }

    val luaMeta = LuaMappedMetatable(EntityApi::class) {
        callable(::isActive)
        callable(::isAwake)
        callable(::isEnabled)
        callable(::setEnabled)
        callable(::setKeepAwake)
        callable(::setSleepWhenUnobserved)
        callable(::getNetworkId)
        callable(::getEntityDefinition)
        callable(::getName)
        callable(::setName)
        callable(::getCoordinate)
        callable(::getFacing)
        callable(::getDimension)
        callable(::getMap)
        callable(::getCollisionViewer)
        callable(::getVisionViewer)
        callable(::getInteractionViewer)
        callable(::spawn)
        callable(::despawn)
        callable(::remove)
        callable(::ref)
        callable(::setCoordinate)
        callable(::setFacing)
        callable(::move)
        callable(::isMoving)
        callable(::getMovementDurationMultiplier)
        callable(::setMovementDurationMultiplier)
        callable(::setVision)
        callable(::getDimension)
        callable(::hasVision)
        callable(::grantVision)
        callable(::revokeVision)
        callable(::setVisibility)
        callable(::isVisible)
        callable(::isInvisible)
        callable(::makeVisible)
        callable(::makeInvisible)
        callable(::hasCollisions)
        callable(::setCollisions)
        callable(::enableCollisions)
        callable(::disableCollisions)
        callable(::setGravityEnabled)
        callable(::hasInteractions)
        callable(::setInteractions)
        callable(::enableInteractions)
        callable(::disableInteractions)
        callable(::addDynamicComponent)
        callable(::getPrimaryControllingPlayer)
        callable(::getControllingPlayers)
        callable(::hasTag)
        callable(::playAnimation)
        callable(::updateVisuals)
        callable(::getRuntimeData)
        callable(::overwriteRuntimeData)
        callable(::hasRuntimeData)
        callable(::removeRuntimeData)
    }

}
