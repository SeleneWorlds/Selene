package world.selene.server.sounds

import party.iroiro.luajava.Lua
import party.iroiro.luajava.value.LuaValue
import world.selene.common.lua.LuaModule
import world.selene.common.lua.util.checkCoordinate
import world.selene.common.lua.util.checkRegistry
import world.selene.common.lua.util.checkType
import world.selene.common.lua.util.checkUserdata
import world.selene.common.lua.util.getFieldFloat
import world.selene.common.lua.util.register
import world.selene.server.dimensions.DimensionApi
import world.selene.server.players.PlayerApi

/**
 * Play local, positional or global sounds.
 */
class SoundsLuaApi(
    private val api: SoundsApi
) : LuaModule {
    override val name = "selene.sounds"

    override fun register(table: LuaValue) {
        table.register("PlaySound", ::luaPlaySound)
        table.register("PlaySoundAt", ::luaPlaySoundAt)
        table.register("PlayGlobalSound", ::luaPlayGlobalSound)
    }

    private fun luaPlaySound(lua: Lua): Int {
        val player = lua.checkUserdata<PlayerApi>(1)
        val sound = lua.checkRegistry(2, api.getSoundRegistry())
        if (lua.top >= 3) lua.checkType(3, Lua.LuaType.TABLE)

        val volume = lua.getFieldFloat(3, "volume") ?: 1f
        val pitch = lua.getFieldFloat(3, "pitch") ?: 1f
        api.playSound(player, sound, volume, pitch)
        return 0
    }

    private fun luaPlaySoundAt(lua: Lua): Int {
        val (coordinate, index) = lua.checkCoordinate(1)
        val sound = lua.checkRegistry(index + 1, api.getSoundRegistry())
        val dimension = if (lua.top >= index + 2) {
            lua.checkUserdata(index + 2, DimensionApi::class).dimension
        } else {
            null
        }
        if (lua.top >= index + 3) lua.checkType(index + 3, Lua.LuaType.TABLE)

        val volume = lua.getFieldFloat(index + 3, "volume") ?: 1f
        val pitch = lua.getFieldFloat(index + 3, "pitch") ?: 1f
        if (dimension != null) {
            api.playSoundAt(coordinate, sound, dimension, volume, pitch)
        } else {
            api.playSoundAt(coordinate, sound, volume = volume, pitch = pitch)
        }
        return 0
    }

    private fun luaPlayGlobalSound(lua: Lua): Int {
        val sound = lua.checkRegistry(1, api.getSoundRegistry())
        val dimension = if (lua.top >= 2) {
            lua.checkUserdata(2, DimensionApi::class).dimension
        } else {
            null
        }
        if (lua.top >= 3) lua.checkType(3, Lua.LuaType.TABLE)

        val volume = lua.getFieldFloat(3, "volume") ?: 1f
        val pitch = lua.getFieldFloat(3, "pitch") ?: 1f
        if (dimension != null) {
            api.playGlobalSound(sound, dimension, volume, pitch)
        } else {
            api.playGlobalSound(sound, volume = volume, pitch = pitch)
        }
        return 0
    }
}
