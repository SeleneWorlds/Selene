package com.seleneworlds.server.players

import com.seleneworlds.common.lua.LuaMappedMetatable
import com.seleneworlds.common.lua.util.*
import com.seleneworlds.server.dimensions.DimensionApi
import com.seleneworlds.server.entities.EntityApi
import party.iroiro.luajava.Lua

object PlayerLuaApi {

    private fun getCustomData(lua: Lua): Int {
        val player = lua.checkUserdata<PlayerApi>(1)
        val identifier = lua.checkIdentifier(2)
        lua.push(player.getCustomData(identifier), Lua.Conversion.FULL)
        return 1
    }

    private fun setCustomData(lua: Lua): Int {
        val player = lua.checkUserdata<PlayerApi>(1)
        val identifier = lua.checkIdentifier(2)
        val value = lua.toObject(3)
        player.setCustomData(identifier, value)
        return 0
    }

    /**
     * Seconds since the server last received input from this player.
     *
     * ```property
     * IdleTime: number
     * ```
     */
    private fun getIdleTime(lua: Lua): Int {
        val player = lua.checkUserdata<PlayerApi>(1)
        lua.push(player.getIdleTime())
        return 1
    }

    /**
     * Unique identifier for this player.
     *
     * ```property
     * UserId: string
     * ```
     */
    private fun getUserId(lua: Lua): Int {
        val player = lua.checkUserdata<PlayerApi>(1)
        lua.push(player.getUserId(), Lua.Conversion.FULL)
        return 1
    }

    /**
     * Locale tag of this player, e.g. `"en_US"`.
     *
     * ```property
     * LocaleString: string
     * ```
     */
    private fun getLocale(lua: Lua): Int {
        val player = lua.checkUserdata<PlayerApi>(1)
        lua.push(player.getLocaleString())
        return 1
    }

    /**
     * Language tag of this player, e.g. `"en"`.
     *
     * ```property
     * LanguageString: string
     * ```
     */
    private fun getLanguage(lua: Lua): Int {
        val player = lua.checkUserdata<PlayerApi>(1)
        lua.push(player.getLanguageString())
        return 1
    }

    /**
     * Entity controlled by this player or `nil`.
     *
     * ```property
     * ControlledEntity: Entity|nil
     * ```
     */
    private fun getControlledEntity(lua: Lua): Int {
        val api = lua.checkUserdata<PlayerApi>(1)
        lua.push(api.getControlledEntity(), Lua.Conversion.NONE)
        return 1
    }

    /**
     * ```property
     * ControlledEntity: Entity
     * ```
     */
    private fun setControlledEntity(lua: Lua): Int {
        val player = lua.checkUserdata<PlayerApi>(1)
        player.setControlledEntity(lua.checkUserdata<EntityApi>(2))
        return 0
    }

    /**
     * Entity backing this player's vision or `nil`.
     *
     * ```property
     * CameraEntity: Entity|nil
     * ```
     */
    private fun getCameraEntity(lua: Lua): Int {
        val player = lua.checkUserdata<PlayerApi>(1)
        lua.push(player.getCameraEntity(), Lua.Conversion.NONE)
        return 1
    }

    /**
     * ```property
     * CameraEntity: Entity
     * ```
     */
    private fun setCameraEntity(lua: Lua): Int {
        val player = lua.checkUserdata<PlayerApi>(1)
        player.setCameraEntity(lua.checkUserdata<EntityApi>(2))
        return 0
    }

    /**
     * Sets the camera to follow the player's controlled entity.
     *
     * ```signatures
     * SetCameraToFollowControlledEntity()
     * ```
     */
    private fun setCameraToFollowControlledEntity(lua: Lua): Int {
        val player = lua.checkUserdata<PlayerApi>(1)
        player.setCameraToFollowControlledEntity()
        return 0
    }

    /**
     * Sets the camera to follow the player's camera target entity.
     *
     * ```signatures
     * SetCameraToFollowTarget()
     * ```
     */
    private fun setCameraToFollowTarget(lua: Lua): Int {
        val player = lua.checkUserdata<PlayerApi>(1)
        player.setCameraToFollowTarget()
        return 0
    }

    /**
     * Sets the camera to focus on a specific coordinate in a dimension.
     *
     * ```signatures
     * SetCameraToCoordinate(coordinate: Coordinate, dimension: Dimension|nil)
     * ```
     */
    private fun setCameraToCoordinate(lua: Lua): Int {
        val player = lua.checkUserdata<PlayerApi>(1)
        val (coordinate, index) = lua.checkCoordinate(2)
        val dimension = lua.toUserdata<DimensionApi>(index + 1)
        player.setCameraToCoordinate(dimension?.dimension, coordinate)
        return 0
    }

    /**
     * Gets a safe reference to this player that provides access to a player without the risk of memory leaks.
     * Player references are backed by the player id. If a player rejoins under a new session, the reference will
     * update to point to their new Player object.
     *
     * ```signatures
     * Ref() -> LuaReference
     * ```
     */
    private fun ref(lua: Lua): Int {
        val player = lua.checkUserdata<PlayerApi>(1)
        lua.push(player.delegate.resolvableReference(), Lua.Conversion.NONE)
        return 1
    }

    val luaMeta = LuaMappedMetatable(PlayerApi::class) {
        callable(::getCustomData)
        callable(::setCustomData)
        callable(::getIdleTime)
        callable(::getUserId)
        callable(::getLocale)
        callable(::getLanguage)
        callable(::getControlledEntity)
        callable(::setControlledEntity)
        callable(::getCameraEntity)
        callable(::setCameraEntity)
        callable(::setCameraToFollowControlledEntity)
        callable(::setCameraToFollowTarget)
        callable(::setCameraToCoordinate)
        callable(::ref)
    }

}
