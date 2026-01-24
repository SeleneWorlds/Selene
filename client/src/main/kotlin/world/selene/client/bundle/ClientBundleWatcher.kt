package world.selene.client.bundle

import org.slf4j.Logger
import world.selene.client.data.Registries
import world.selene.common.bundles.BundleDatabase
import world.selene.common.bundles.BundleWatcher
import world.selene.common.data.Identifier
import world.selene.common.data.Registry

class ClientBundleWatcher(
    logger: Logger,
    bundleDatabase: BundleDatabase,
    private val registries: Registries
) : BundleWatcher(logger, bundleDatabase) {

    override fun getRegistry(name: String): Registry<*>? {
        val builtinRegistry = registries.getRegistry(Identifier.withDefaultNamespace(name))
        if (builtinRegistry != null) {
            return builtinRegistry
        }
        return registries.customRegistries.findByRegistryName(name)
    }

}
