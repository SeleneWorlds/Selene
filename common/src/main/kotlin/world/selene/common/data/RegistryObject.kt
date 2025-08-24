package world.selene.common.data

interface RegistryObject<T : Any> {
    val name: String
    val registry: Registry<T>
    val id: Int
    fun initializeFromRegistry(registry: Registry<T>, name: String, id: Int)
}
