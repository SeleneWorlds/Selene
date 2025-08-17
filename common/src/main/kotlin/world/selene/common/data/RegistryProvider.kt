package world.selene.common.data

interface RegistryProvider {
    fun getRegistry(name: String): Registry<*>?
}
