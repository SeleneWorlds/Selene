@file:Suppress("SameReturnValue", "unused", "RedundantSuppression")

package world.selene.client.sounds

import world.selene.client.data.Registries
import world.selene.common.sounds.SoundDefinition

/**
 * Play or stop local sounds.
 */
class SoundsApi(
    private val registries: Registries,
    private val soundManager: SoundManager
) {
    fun playSound(sound: SoundDefinition, volume: Float = 1f, pitch: Float = 1f) {
        soundManager.playSound(sound, volume, pitch)
    }

    fun stopSound(sound: SoundDefinition) {
        soundManager.stopSound(sound)
    }

    fun stopAllSounds() {
        soundManager.stopAllSounds()
    }

    fun getSoundRegistry() = registries.sounds
}
