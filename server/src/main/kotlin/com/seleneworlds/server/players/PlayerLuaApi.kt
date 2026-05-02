package com.seleneworlds.server.players

import party.iroiro.luajava.Lua
import com.seleneworlds.common.lua.LuaMappedMetatable
import com.seleneworlds.common.lua.util.checkCoordinate
import com.seleneworlds.common.lua.util.checkUserdata
import com.seleneworlds.common.lua.util.toUserdata
import com.seleneworlds.server.dimensions.DimensionApi
import com.seleneworlds.server.entities.EntityApi

object PlayerLuaApi {

    /**
     * Observable map for storing data on this player.
     *
     * ```property
     * CustomData: ObservableMap
     * ```
     */
    private fun luaGetCustomData(lua: Lua): Int {
        val player = lua.checkUserdata<PlayerApi>(1)
        lua.push(player.getCustomData(), Lua.Conversion.NONE)
        return 1
    }

    /**
     * Seconds since the server last received input from this player.
     *
     * ```property
     * IdleTime: number
     * ```
     */
    private fun luaGetIdleTime(lua: Lua): Int {
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
    private fun luaGetUserId(lua: Lua): Int {
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
    private fun luaGetLocaleString(lua: Lua): Int {
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
    private fun luaGetLanguageString(lua: Lua): Int {
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
    private fun luaGetControlledEntity(lua: Lua): Int {
        val player = lua.checkUserdata<PlayerApi>(1)
        lua.push(player.getControlledEntity(), Lua.Conversion.NONE)
        return 1
    }

    /**
     * ```property
     * ControlledEntity: Entity
     * ```
     */
    private fun luaSetControlledEntity(lua: Lua): Int {
        val player = lua.checkUserdata<PlayerApi>(1)
        player.setControlledEntity(lua.checkUserdata<EntityApi>(3))
        return 0
    }

    /**
     * Entity backing this player's vision or `nil`.
     *
     * ```property
     * CameraEntity: Entity|nil
     * ```
     */
    private fun luaGetCameraEntity(lua: Lua): Int {
        val player = lua.checkUserdata<PlayerApi>(1)
        lua.push(player.getCameraEntity(), Lua.Conversion.NONE)
        return 1
    }

    /**
     * ```property
     * CameraEntity: Entity
     * ```
     */
    private fun luaSetCameraEntity(lua: Lua): Int {
        val player = lua.checkUserdata<PlayerApi>(1)
        player.setCameraEntity(lua.checkUserdata<EntityApi>(3))
        return 0
    }

    /**
     * Sets the camera to follow the player's controlled entity.
     *
     * ```signatures
     * SetCameraToFollowControlledEntity()
     * ```
     */
    private fun luaSetCameraToFollowControlledEntity(lua: Lua): Int {
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
    private fun luaSetCameraToFollowTarget(lua: Lua): Int {
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
    private fun luaSetCameraToCoordinate(lua: Lua): Int {
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
    private fun luaRef(lua: Lua): Int {
        val player = lua.checkUserdata<PlayerApi>(1)
        lua.push(player.delegate.resolvableReference(), Lua.Conversion.NONE)
        return 1
    }

    val luaMeta = LuaMappedMetatable(PlayerApi::class) {
        getter(::luaGetCustomData)
        getter(::luaGetIdleTime)
        getter(::luaGetUserId)
        getter(::luaGetLocaleString, "Locale")
        getter(::luaGetLanguageString, "Language")
        getter(::luaGetControlledEntity)
        setter(::luaSetControlledEntity)
        getter(::luaGetCameraEntity)
        setter(::luaSetCameraEntity)
        callable(::luaSetCameraToFollowControlledEntity)
        callable(::luaSetCameraToFollowTarget)
        callable(::luaSetCameraToCoordinate)
        callable(::luaRef)
    }

}
