package world.selene.common.data

import kotlin.reflect.KClass

interface Registry<TData : Any> {
    val clazz: KClass<TData>
    val name: String
    fun get(id: Int): TData?
    fun get(name: String): TData?
    fun getAll(): Map<String, TData>
    fun findByMetadata(key: String, value: Any): Pair<String, TData>?
    fun registryPopulated(mappings: NameIdRegistry) = Unit
}