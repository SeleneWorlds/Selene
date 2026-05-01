@file:Suppress("SameReturnValue", "unused", "RedundantSuppression")

package world.selene.client.sounds

import party.iroiro.luajava.Lua
import world.selene.client.data.Registries
import world.selene.common.lua.util.checkRegistry
import world.selene.common.lua.util.checkType
import world.selene.common.lua.util.getFieldFloat

/**
 * Play or stop local sounds.
 */
class SoundsApi(
    private val registries: Registries,
    private val soundManager: SoundManager
) {
    fun luaPlaySound(lua: Lua): Int {
        val sound = lua.checkRegistry(1, registries.sounds)
        if (lua.top >= 2) lua.checkType(2, Lua.LuaType.TABLE)

        val volume = lua.getFieldFloat(2, "volume") ?: 1f
        val pitch = lua.getFieldFloat(2, "pitch") ?: 1f

        soundManager.playSound(sound, volume, pitch)
        return 0
    }

    fun luaStopSound(lua: Lua): Int {
        val sound = lua.checkRegistry(1, registries.sounds)
        soundManager.stopSound(sound)
        return 0
    }

    fun luaStopAllSounds(lua: Lua): Int {
        soundManager.stopAllSounds()
        return 0
    }
}
