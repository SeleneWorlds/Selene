package world.selene.client.lua

import party.iroiro.luajava.Lua
import party.iroiro.luajava.value.LuaValue
import world.selene.client.data.Registries
import world.selene.client.sound.SoundManager
import world.selene.common.lua.LuaModule
import world.selene.common.lua.checkString
import world.selene.common.lua.checkType
import world.selene.common.lua.getFieldFloat
import world.selene.common.lua.register

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

    private fun luaPlaySound(lua: Lua): Int {
        val soundName = lua.checkString(1)
        if (lua.top >= 2) lua.checkType(2, Lua.LuaType.TABLE)
        
        val volume = lua.getFieldFloat(2, "volume") ?: 1f
        val pitch = lua.getFieldFloat(2, "pitch") ?: 1f

        val soundId = registries.mappings.getId("sounds", soundName)
        if (soundId == null) {
            return lua.error(IllegalArgumentException("Could not find sound with name $soundName"))
        }
        soundManager.playSound(soundId, volume, pitch)

        return 0
    }

    private fun luaStopSound(lua: Lua): Int {
        val soundName = lua.checkString(1)
        val soundId = registries.mappings.getId("sounds", soundName)
        if (soundId == null) {
            return lua.error(IllegalArgumentException("Could not find sound with name $soundName"))
        }
        soundManager.stopSound(soundId)
        return 0
    }

    private fun luaStopAllSounds(lua: Lua): Int {
        soundManager.stopAllSounds()
        return 0
    }

}
