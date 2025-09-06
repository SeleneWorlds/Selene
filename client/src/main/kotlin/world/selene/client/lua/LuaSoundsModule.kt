package world.selene.client.lua

import party.iroiro.luajava.Lua
import party.iroiro.luajava.value.LuaValue
import world.selene.client.data.Registries
import world.selene.client.sound.SoundManager
import world.selene.common.lua.LuaModule
import world.selene.common.lua.checkRegistry
import world.selene.common.lua.checkString
import world.selene.common.lua.checkType
import world.selene.common.lua.getFieldFloat
import world.selene.common.lua.register
import world.selene.common.lua.throwError

/**
 * Play or stop local sounds.
 */
class LuaSoundsModule(
    private val registries: Registries,
    private val soundManager: SoundManager
) : LuaModule {
    override val name = "selene.sounds"

    override fun register(table: LuaValue) {
        table.register("PlaySound", this::luaPlaySound)
        table.register("StopSound", this::luaStopSound)
        table.register("StopAllSounds", this::luaStopAllSounds)
    }

    /**
     * Plays a sound with optional volume and pitch settings.
     *
     * ```signatures
     * PlaySound(sound: Sound, options: table{volume: number, pitch: number})
     * ```
     */
    private fun luaPlaySound(lua: Lua): Int {
        val sound = lua.checkRegistry(1, registries.sounds)
        if (lua.top >= 2) lua.checkType(2, Lua.LuaType.TABLE)
        
        val volume = lua.getFieldFloat(2, "volume") ?: 1f
        val pitch = lua.getFieldFloat(2, "pitch") ?: 1f

        soundManager.playSound(sound, volume, pitch)
        return 0
    }

    /**
     * Stops all instances of a specific sound.
     *
     * ```signatures
     * StopSound(sound: Sound)
     * ```
     */
    private fun luaStopSound(lua: Lua): Int {
        val sound = lua.checkRegistry(1, registries.sounds)
        soundManager.stopSound(sound)
        return 0
    }

    /**
     * Stops all currently playing sounds.
     *
     * ```signatures
     * StopAllSounds()
     * ```
     */
    private fun luaStopAllSounds(lua: Lua): Int {
        soundManager.stopAllSounds()
        return 0
    }

}
