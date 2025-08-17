package world.selene.client.sound

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.audio.Sound
import world.selene.client.assets.BundleFileResolver
import world.selene.client.data.Registries
import world.selene.client.data.SimpleSoundDefinition
import java.util.concurrent.ConcurrentHashMap

class SoundManager(
    private val registries: Registries,
    private val bundleFileResolver: BundleFileResolver
) {
    private val loadedSounds = ConcurrentHashMap<String, Sound>()
    private val playingSounds = ConcurrentHashMap<String, Long>()

    fun playSound(soundName: String, volume: Float = 1f, pitch: Float = 1f) {
        val soundDef = registries.sounds.get(soundName) as? SimpleSoundDefinition
        if (soundDef != null) {
            playSimpleSound(soundName, soundDef, volume * soundDef.volume, pitch * soundDef.pitch)
        }
    }

    fun stopSound(soundName: String) {
        playingSounds[soundName]?.let { soundId ->
            loadedSounds.values.forEach { sound ->
                sound.stop(soundId)
            }
            playingSounds.remove(soundName)
        }
    }

    fun stopAllSounds() {
        playingSounds.clear()
        loadedSounds.values.forEach { it.stop() }
    }

    private fun playSimpleSound(soundName: String, soundDef: SimpleSoundDefinition, volume: Float, pitch: Float) {
        val sound = getOrLoadSound(soundDef.file)
        val soundId = if (soundDef.loop) {
            sound.loop(volume, pitch, 0f)
        } else {
            sound.play(volume, pitch, 0f)
        }
        playingSounds[soundName] = soundId
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

    fun dispose() {
        stopAllSounds()
        loadedSounds.values.forEach { it.dispose() }
        loadedSounds.clear()
    }
}
