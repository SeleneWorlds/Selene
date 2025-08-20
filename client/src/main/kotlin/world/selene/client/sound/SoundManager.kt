package world.selene.client.sound

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.audio.Sound
import org.slf4j.Logger
import world.selene.client.assets.BundleFileResolver
import world.selene.client.data.Registries
import world.selene.client.data.SimpleAudioDefinition
import world.selene.common.util.Disposable
import java.util.concurrent.ConcurrentHashMap

class SoundManager(
    private val registries: Registries,
    private val logger: Logger,
    private val bundleFileResolver: BundleFileResolver
) : Disposable {
    private val loadedSounds = ConcurrentHashMap<String, Sound>()
    private val playingSounds = ConcurrentHashMap<String, Long>()

    fun playSound(soundId: Int, volume: Float = 1f, pitch: Float = 1f) {
        val soundName = registries.mappings.getName("sounds", soundId)
        if (soundName == null) {
            logger.warn("Could not find sound with id $soundId")
            return
        }

        val soundDef = registries.sounds.get(soundName) as? SimpleAudioDefinition
        if (soundDef != null) {
            playSimpleSound(soundName, soundDef, volume * soundDef.volume, pitch * soundDef.pitch)
        }
    }

    fun stopSound(soundId: Int) {
        val soundName = registries.mappings.getName("sounds", soundId)
        if (soundName == null) {
            return
        }

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

    private fun playSimpleSound(soundName: String, soundDef: SimpleAudioDefinition, volume: Float, pitch: Float) {
        val sound = getOrLoadSound(soundDef.file)
        val instanceId = if (soundDef.loop) {
            sound.loop(volume, pitch, 0f)
        } else {
            sound.play(volume, pitch, 0f)
        }
        playingSounds[soundName] = instanceId
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
