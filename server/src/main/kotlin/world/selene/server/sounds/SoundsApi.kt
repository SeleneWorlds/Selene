package world.selene.server.sounds

import party.iroiro.luajava.Lua
import world.selene.common.lua.util.checkCoordinate
import world.selene.common.lua.util.checkRegistry
import world.selene.common.lua.util.checkType
import world.selene.common.lua.util.checkUserdata
import world.selene.common.lua.util.getFieldFloat
import world.selene.common.network.packet.PlaySoundPacket
import world.selene.server.data.Registries
import world.selene.server.dimensions.DimensionApi
import world.selene.server.players.Player
import world.selene.server.players.PlayerApi
import world.selene.server.world.World

/**
 * Play local, positional or global sounds.
 */
@Suppress("SameReturnValue")
class SoundsApi(
    private val registries: Registries,
    private val world: World
) {
    fun luaPlaySound(lua: Lua): Int {
        val player = lua.checkUserdata<PlayerApi>(1)
        val sound = lua.checkRegistry(2, registries.sounds)
        if (lua.top >= 3) lua.checkType(3, Lua.LuaType.TABLE)

        val volume = lua.getFieldFloat(3, "volume") ?: 1f
        val pitch = lua.getFieldFloat(3, "pitch") ?: 1f

        player.delegate.client.send(PlaySoundPacket(sound.id, volume, pitch, null))
        return 0
    }

    fun luaPlaySoundAt(lua: Lua): Int {
        val (coordinate, index) = lua.checkCoordinate(1)
        val sound = lua.checkRegistry(index + 1, registries.sounds)
        val dimension = if (lua.top >= index + 2) {
            lua.checkUserdata(index + 2, DimensionApi::class).dimension
        } else {
            world.dimensionManager.getOrCreateDimension(0)
        }
        if (lua.top >= index + 3) lua.checkType(index + 3, Lua.LuaType.TABLE)

        val volume = lua.getFieldFloat(index + 3, "volume") ?: 1f
        val pitch = lua.getFieldFloat(index + 3, "pitch") ?: 1f

        val packet = PlaySoundPacket(sound.id, volume, pitch, coordinate)
        dimension.syncManager.sendToAllWatching(coordinate, packet)
        return 0
    }

    fun luaPlayGlobalSound(lua: Lua): Int {
        val sound = lua.checkRegistry(1, registries.sounds)
        val dimension = if (lua.top >= 2) {
            lua.checkUserdata(2, DimensionApi::class).dimension
        } else {
            world.dimensionManager.getOrCreateDimension(0)
        }
        if (lua.top >= 3) lua.checkType(3, Lua.LuaType.TABLE)

        val volume = lua.getFieldFloat(3, "volume") ?: 1f
        val pitch = lua.getFieldFloat(3, "pitch") ?: 1f

        dimension.syncManager.sendToAll(PlaySoundPacket(sound.id, volume, pitch, null))
        return 0
    }
}
