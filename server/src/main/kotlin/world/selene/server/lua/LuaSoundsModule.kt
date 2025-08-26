package world.selene.server.lua

import party.iroiro.luajava.Lua
import party.iroiro.luajava.value.LuaValue
import world.selene.common.lua.LuaModule
import world.selene.common.lua.checkCoordinate
import world.selene.common.lua.checkUserdata
import world.selene.common.lua.checkString
import world.selene.common.lua.checkType
import world.selene.common.lua.getFieldFloat
import world.selene.common.lua.register
import world.selene.common.network.packet.PlaySoundPacket
import world.selene.server.data.Registries
import world.selene.server.dimensions.Dimension
import world.selene.server.player.Player
import world.selene.server.world.World

class LuaSoundsModule(
    private val registries: Registries,
    private val world: World
) : LuaModule {
    override val name = "selene.sounds"

    override fun register(table: LuaValue) {
        table.register("PlaySound", this::luaPlaySound)
        table.register("PlaySoundAt", this::luaPlaySoundAt)
        table.register("PlayGlobalSound", this::luaPlayGlobalSound)
    }

    private fun luaPlaySound(lua: Lua): Int {
        val player = lua.checkUserdata<Player>(1)
        val soundName = lua.checkString(2)
        if (lua.top >= 3) lua.checkType(3, Lua.LuaType.TABLE)

        val volume = lua.getFieldFloat(3, "volume") ?: 1f
        val pitch = lua.getFieldFloat(3, "pitch") ?: 1f

        val soundId = registries.mappings.getId("sounds", soundName)
            ?: return lua.error(IllegalArgumentException("Could not find sound with name $soundName"))
        player.client.send(PlaySoundPacket(soundId, volume, pitch, null))
        return 0
    }

    private fun luaPlaySoundAt(lua: Lua): Int {
        val (coordinate, index) = lua.checkCoordinate(1)
        val soundName = lua.checkString(index + 1)
        val dimension = if (lua.top >= index + 2) lua.checkUserdata(
            index + 2,
            Dimension::class
        ) else world.dimensionManager.getOrCreateDimension(0)
        if (lua.top >= index + 3) lua.checkType(index + 3, Lua.LuaType.TABLE)

        val volume = lua.getFieldFloat(index + 2, "volume") ?: 1f
        val pitch = lua.getFieldFloat(index + 2, "pitch") ?: 1f

        val soundId = registries.mappings.getId("sounds", soundName)
            ?: return lua.error(IllegalArgumentException("Could not find sound with name $soundName"))

        val packet = PlaySoundPacket(soundId, volume, pitch, coordinate)
        dimension.syncManager.sendToAllWatching(coordinate, packet)
        return 0
    }

    private fun luaPlayGlobalSound(lua: Lua): Int {
        val soundName = lua.checkString(1)
        val dimension = if (lua.top >= 2) lua.checkUserdata(
            2,
            Dimension::class
        ) else world.dimensionManager.getOrCreateDimension(0)
        if (lua.top >= 3) lua.checkType(3, Lua.LuaType.TABLE)

        val volume = lua.getFieldFloat(2, "volume") ?: 1f
        val pitch = lua.getFieldFloat(2, "pitch") ?: 1f

        val soundId = registries.mappings.getId("sounds", soundName)
            ?: return lua.error(IllegalArgumentException("Could not find sound with name $soundName"))
        dimension.syncManager.sendToAll(PlaySoundPacket(soundId, volume, pitch, null))
        return 0
    }

}
