package com.seleneworlds.server.sounds

import com.seleneworlds.common.grid.Coordinate
import com.seleneworlds.common.network.packet.PlaySoundPacket
import com.seleneworlds.common.sounds.SoundDefinition
import com.seleneworlds.server.dimensions.Dimension
import com.seleneworlds.server.data.Registries
import com.seleneworlds.server.players.PlayerApi
import com.seleneworlds.server.world.World

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
        coordinate: Coordinate,
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
