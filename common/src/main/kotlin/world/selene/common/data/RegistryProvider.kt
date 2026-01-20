package world.selene.common.data

interface RegistryProvider {
    @Deprecated("Use getRegistry(identifier) instead", ReplaceWith("getRegistry(Identifier.parse(name))"))
    fun getRegistry(name: String): Registry<*>? = getRegistry(Identifier.parse(name))
    fun getRegistry(identifier: Identifier): Registry<*>?
}
