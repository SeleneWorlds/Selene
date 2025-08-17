package world.selene.server.lua

import party.iroiro.luajava.Lua
import party.iroiro.luajava.value.LuaValue
import world.selene.common.lua.LuaModule
import world.selene.common.lua.checkCoordinate
import world.selene.common.lua.checkJavaObject
import world.selene.common.lua.checkString
import world.selene.common.lua.checkType
import world.selene.common.lua.getFieldFloat
import world.selene.common.lua.register
import world.selene.common.network.packet.PlaySoundPacket
import world.selene.server.player.Player
import world.selene.server.network.NetworkServer

class LuaSoundsModule(private val networkServer: NetworkServer) : LuaModule {
    override val name = "selene.sounds"

    override fun register(table: LuaValue) {
        table.register("PlaySound", this::luaPlaySound)
        table.register("PlaySoundAt", this::luaPlaySoundAt)
        table.register("PlayGlobalSound", this::luaPlayGlobalSound)
    }

    private fun luaPlaySound(lua: Lua): Int {
        val player = lua.checkJavaObject<Player.PlayerLuaProxy>(1).delegate
        val soundName = lua.checkString(2)
        if (lua.top >= 3) lua.checkType(3, Lua.LuaType.TABLE)

        val volume = lua.getFieldFloat(3, "volume") ?: 1f
        val pitch = lua.getFieldFloat(3, "pitch") ?: 1f

        player.client.send(PlaySoundPacket(soundName, volume, pitch, null))
        return 0
    }

    private fun luaPlaySoundAt(lua: Lua): Int {
        val (coordinate, index) = lua.checkCoordinate(1)
        val soundName = lua.checkString(index + 1)
        if (lua.top >= index + 2) lua.checkType(index + 2, Lua.LuaType.TABLE)

        val volume = lua.getFieldFloat(index + 2, "volume") ?: 1f
        val pitch = lua.getFieldFloat(index + 2, "pitch") ?: 1f

        // TODO Check if player is nearby to hear
        networkServer.clients.forEach { client ->
            client.send(PlaySoundPacket(soundName, volume, pitch, coordinate))
        }
        return 0
    }

    private fun luaPlayGlobalSound(lua: Lua): Int {
        val soundName = lua.checkString(1)
        if (lua.top >= 2) lua.checkType(2, Lua.LuaType.TABLE)

        val volume = lua.getFieldFloat(2, "volume") ?: 1f
        val pitch = lua.getFieldFloat(2, "pitch") ?: 1f

        // TODO prettier way to broadcast needed
        networkServer.clients.forEach { client ->
            client.send(PlaySoundPacket(soundName, volume, pitch, null))
        }
        return 0
    }

}
