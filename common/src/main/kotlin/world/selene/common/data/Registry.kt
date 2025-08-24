package world.selene.common.data

interface Registry<TData : Any> {
    val name: String
    fun get(id: Int): TData?
    fun get(name: String): TData?
    fun getAll(): Map<String, TData>
    fun registryPopulated(mappings: NameIdRegistry)
}