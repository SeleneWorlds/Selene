package world.selene.common.data

class RegistriesApi(private val registryProvider: RegistryProvider) {

    fun findAll(registryName: String): Map<Identifier, *> {
        val registry = registryProvider.getRegistry(Identifier.parse(registryName))
            ?: throw IllegalArgumentException("Unknown registry: $registryName")
        return registry.getAll()
    }

    fun findByMetadata(registryName: String, key: String, value: Any): Any? {
        val registry = registryProvider.getRegistry(Identifier.parse(registryName))
            ?: throw IllegalArgumentException("Unknown registry: $registryName")
        return registry.findByMetadata(key, value)?.second
    }

    fun findByName(registryName: String, name: String): Any? {
        val registry = registryProvider.getRegistry(Identifier.parse(registryName))
            ?: throw IllegalArgumentException("Unknown registry: $registryName")
        return registry.get(Identifier.parse(name))
    }
}
