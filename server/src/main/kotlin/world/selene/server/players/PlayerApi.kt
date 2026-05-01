package world.selene.server.players

import party.iroiro.luajava.Lua
import world.selene.common.grid.Coordinate
import world.selene.common.lua.IdResolvable
import world.selene.common.lua.LuaMetatable
import world.selene.common.lua.LuaMetatableProvider
import world.selene.common.lua.ResolvableReference
import world.selene.common.observable.ObservableMap
import world.selene.server.dimensions.Dimension
import world.selene.server.entities.EntityApi

class PlayerApi(val player: Player) : IdResolvable<String, Player>, LuaMetatableProvider {

    fun getCustomData(): ObservableMap {
        return player.customData
    }

    fun getIdleTime(): Int {
        return player.idleTime
    }

    fun getUserId(): String? {
        return player.userId
    }

    fun getLocaleString(): String {
        return player.localeString
    }

    fun getLanguageString(): String {
        return player.languageString
    }

    fun getControlledEntity(): EntityApi? {
        return player.controlledEntity?.api
    }

    fun setControlledEntity(entity: EntityApi) {
        player.controlledEntity = entity.entity
    }

    fun getCameraEntity(): EntityApi? {
        return player.cameraEntity?.api
    }

    fun setCameraEntity(entity: EntityApi) {
        player.cameraEntity = entity.entity
    }

    fun setCameraToFollowControlledEntity() {
        player.setCameraToFollowControlledEntity()
    }

    fun setCameraToFollowTarget() {
        player.setCameraToFollowTarget()
    }

    fun setCameraToCoordinate(dimension: Dimension?, coordinate: Coordinate) {
        player.setCameraToCoordinate(dimension ?: player.camera.dimension ?: player.dimensionManager.getOrCreateDimension(0), coordinate)
    }

    override fun resolvableReference(): ResolvableReference<String, Player> {
        return player.resolvableReference()
    }

    override fun luaMetatable(lua: Lua): LuaMetatable {
        return PlayerLuaApi.luaMeta
    }

}
