package world.selene.client.sounds

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.audio.Sound
import org.slf4j.Logger
import world.selene.client.bundles.BundleFileResolver
import world.selene.client.data.Registries
import world.selene.common.sounds.SoundDefinition
import world.selene.common.util.Disposable
import java.util.concurrent.ConcurrentHashMap

class SoundManager(
    private val registries: Registries,
    private val logger: Logger,
    private val bundleFileResolver: BundleFileResolver
) : Disposable {
    private val loadedSounds = ConcurrentHashMap<String, Sound>()
    private val playingSounds = ConcurrentHashMap<Int, Long>()

    fun playSound(sound: SoundDefinition, volume: Float = 1f, pitch: Float = 1f) {
        val audio = registries.audios.get(sound.audio)
        if (audio is SimpleAudioDefinition) {
            playSimpleSound(sound, audio, volume * audio.volume, pitch * audio.pitch)
        } else {
            logger.warn("Could not find audio with id ${sound.audio}")
        }
    }

    fun stopSound(soundDefinition: SoundDefinition) {
        playingSounds[soundDefinition.id]?.let { soundId ->
            loadedSounds.values.forEach { sound ->
                sound.stop(soundId)
            }
            playingSounds.remove(soundDefinition.id)
        }
    }

    fun stopAllSounds() {
        playingSounds.clear()
        loadedSounds.values.forEach { it.stop() }
    }

    private fun playSimpleSound(
        soundDefinition: SoundDefinition,
        audio: SimpleAudioDefinition,
        volume: Float,
        pitch: Float
    ) {
        val sound = getOrLoadSound(audio.file)
        val instanceId = if (audio.loop) {
            sound.loop(volume, pitch, 0f)
        } else {
            sound.play(volume, pitch, 0f)
        }
        playingSounds[soundDefinition.id] = instanceId
    }

    private fun getOrLoadSound(filePath: String): Sound {
        return loadedSounds.getOrPut(filePath) {
            val resolvedFile = bundleFileResolver.resolve(filePath)
            if (resolvedFile.exists()) {
                Gdx.audio.newSound(resolvedFile)
            } else {
                throw RuntimeException("Sound file not found: $filePath")
            }
        }
    }

    override fun dispose() {
        stopAllSounds()
        loadedSounds.values.forEach { it.dispose() }
        loadedSounds.clear()
    }
}
