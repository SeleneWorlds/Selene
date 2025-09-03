package world.selene.server.lua

import party.iroiro.luajava.Lua
import party.iroiro.luajava.value.LuaValue
import world.selene.common.lua.LuaModule
import world.selene.common.lua.checkCoordinate
import world.selene.common.lua.checkRegistry
import world.selene.common.lua.checkUserdata
import world.selene.common.lua.checkType
import world.selene.common.lua.getFieldFloat
import world.selene.common.lua.register
import world.selene.common.network.packet.PlaySoundPacket
import world.selene.server.data.Registries
import world.selene.server.dimensions.Dimension
import world.selene.server.player.Player
import world.selene.server.world.World

/**
 * Provides functions for playing sounds.
 */
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

    /**
     * Plays a sound to a specific player.
     *
     * ```lua
     * PlaySound(Player player, Sound sound, {number volume, number pitch} options)
     * ```
     */
    private fun luaPlaySound(lua: Lua): Int {
        val player = lua.checkUserdata<Player>(1)
        val sound = lua.checkRegistry(2, registries.sounds)
        if (lua.top >= 3) lua.checkType(3, Lua.LuaType.TABLE)

        val volume = lua.getFieldFloat(3, "volume") ?: 1f
        val pitch = lua.getFieldFloat(3, "pitch") ?: 1f

        player.client.send(PlaySoundPacket(sound.id, volume, pitch, null))
        return 0
    }

    /**
     * Plays a positional sound at a coordinate to all nearby players in the dimension.
     *
     * ```lua
     * PlaySoundAt(Coordinate coordinate, Sound sound, Dimension dimension, {number volume, number pitch} options)
     * ```
     */
    private fun luaPlaySoundAt(lua: Lua): Int {
        val (coordinate, index) = lua.checkCoordinate(1)
        val sound = lua.checkRegistry(index + 1, registries.sounds)
        val dimension = if (lua.top >= index + 2) lua.checkUserdata(
            index + 2,
            Dimension::class
        ) else world.dimensionManager.getOrCreateDimension(0)
        if (lua.top >= index + 3) lua.checkType(index + 3, Lua.LuaType.TABLE)

        val volume = lua.getFieldFloat(index + 2, "volume") ?: 1f
        val pitch = lua.getFieldFloat(index + 2, "pitch") ?: 1f

        val packet = PlaySoundPacket(sound.id, volume, pitch, coordinate)
        dimension.syncManager.sendToAllWatching(coordinate, packet)
        return 0
    }

    /**
     * Plays a sound to all players in a dimension.
     *
     * ```lua
     * PlayGlobalSound(Sound sound, Dimension dimension, {number volume, number pitch} options)
     * ```
     */
    private fun luaPlayGlobalSound(lua: Lua): Int {
        val sound = lua.checkRegistry(1, registries.sounds)
        val dimension = if (lua.top >= 2) lua.checkUserdata(
            2,
            Dimension::class
        ) else world.dimensionManager.getOrCreateDimension(0)
        if (lua.top >= 3) lua.checkType(3, Lua.LuaType.TABLE)

        val volume = lua.getFieldFloat(2, "volume") ?: 1f
        val pitch = lua.getFieldFloat(2, "pitch") ?: 1f

        dimension.syncManager.sendToAll(PlaySoundPacket(sound.id, volume, pitch, null))
        return 0
    }

}
