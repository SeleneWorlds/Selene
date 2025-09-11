package world.selene.client.bundles

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.assets.loaders.FileHandleResolver
import com.badlogic.gdx.files.FileHandle
import world.selene.common.bundles.BundleDatabase

class BundleFileResolver(private val bundleDatabase: BundleDatabase) : FileHandleResolver {

    override fun resolve(fileName: String): FileHandle {
        return bundleDatabase.loadedBundles.asSequence().mapNotNull {
            val handle = Gdx.files.absolute(it.dir.absolutePath).child(fileName)
            if (handle.exists()) handle else null
        }.lastOrNull() ?: Gdx.files.internal(fileName)
    }
}