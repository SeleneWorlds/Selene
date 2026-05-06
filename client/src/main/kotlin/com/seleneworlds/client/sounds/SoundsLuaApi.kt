package com.seleneworlds.client.sounds

import party.iroiro.luajava.Lua
import party.iroiro.luajava.value.LuaValue
import com.seleneworlds.common.lua.LuaModule
import com.seleneworlds.common.lua.util.checkRegistry
import com.seleneworlds.common.lua.util.checkType
import com.seleneworlds.common.lua.util.getFieldFloat
import com.seleneworlds.common.lua.util.register

/**
 * Play or stop local sounds.
 */
class SoundsLuaApi(
    private val api: SoundsApi
) : LuaModule {
    override val name = "selene.sounds"

    override fun register(table: LuaValue) {
        table.register("playSound", ::playSound)
        table.register("stopSound", ::stopSound)
        table.register("stopAllSounds", ::stopAllSounds)
    }

    private fun playSound(lua: Lua): Int {
        val sound = lua.checkRegistry(1, api.getSoundRegistry())
        if (lua.top >= 2) lua.checkType(2, Lua.LuaType.TABLE)

        val volume = lua.getFieldFloat(2, "volume") ?: 1f
        val pitch = lua.getFieldFloat(2, "pitch") ?: 1f
        api.playSound(sound, volume, pitch)
        return 0
    }

    private fun stopSound(lua: Lua): Int {
        api.stopSound(lua.checkRegistry(1, api.getSoundRegistry()))
        return 0
    }

    private fun stopAllSounds(lua: Lua): Int {
        api.stopAllSounds()
        return 0
    }
}
