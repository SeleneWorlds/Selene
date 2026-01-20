package world.selene.common.data

import world.selene.common.data.mappings.NameIdRegistry
import kotlin.reflect.KClass

interface Registry<TData : Any> {
    val clazz: KClass<TData>
    val name: String
    fun get(id: Int): TData?
    @Deprecated("Use get(identifier) instead", ReplaceWith("get(Identifier.parse(name))"))
    fun get(name: String): TData? {
        return get(Identifier.parse(name))
    }
    fun get(identifier: Identifier): TData?
    fun getAll(): Map<Identifier, TData>
    fun findByMetadata(key: String, value: Any): Pair<Identifier, TData>?
    fun registryPopulated(mappings: NameIdRegistry, throwOnMissingId: Boolean = true) = Unit
}