package world.selene.server.sounds

import world.selene.common.network.packet.PlaySoundPacket
import world.selene.common.sounds.SoundDefinition
import world.selene.server.dimensions.Dimension
import world.selene.server.data.Registries
import world.selene.server.players.PlayerApi
import world.selene.server.world.World

/**
 * Play local, positional or global sounds.
 */
class SoundsApi(
    private val registries: Registries,
    private val world: World
) {
    fun playSound(player: PlayerApi, sound: SoundDefinition, volume: Float = 1f, pitch: Float = 1f) {
        player.delegate.client.send(PlaySoundPacket(sound.id, volume, pitch, null))
    }

    fun playSoundAt(
        coordinate: world.selene.common.grid.Coordinate,
        sound: SoundDefinition,
        dimension: Dimension = world.dimensionManager.getOrCreateDimension(0),
        volume: Float = 1f,
        pitch: Float = 1f
    ) {
        val packet = PlaySoundPacket(sound.id, volume, pitch, coordinate)
        dimension.syncManager.sendToAllWatching(coordinate, packet)
    }

    fun playGlobalSound(
        sound: SoundDefinition,
        dimension: Dimension = world.dimensionManager.getOrCreateDimension(0),
        volume: Float = 1f,
        pitch: Float = 1f
    ) {
        dimension.syncManager.sendToAll(PlaySoundPacket(sound.id, volume, pitch, null))
    }

    fun getSoundRegistry() = registries.sounds
}
