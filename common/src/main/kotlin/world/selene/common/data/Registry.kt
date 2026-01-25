package world.selene.common.data

import world.selene.common.data.mappings.NameIdRegistry
import kotlin.reflect.KClass

interface Registry<TData : Any> {
    val clazz: KClass<TData>
    val name: String
    fun get(id: Int): TData?
    fun getIdentifier(id: Int): Identifier?
    @Deprecated("Use get(identifier) instead", ReplaceWith("get(Identifier.parse(name))"))
    fun get(name: String): TData? {
        return get(Identifier.parse(name))
    }
    fun get(identifier: Identifier): TData?
    fun getReference(identifier: Identifier): RegistryReference<TData> {
        return RegistryReference.ByIdentifier(this, identifier)
    }
    fun getReference(id: Int): RegistryReference<TData> {
        return getIdentifier(id)?.let { getReference(it) } ?: RegistryReference.unbound()
    }
    fun getId(identifier: Identifier): Int
    fun getAll(): Map<Identifier, TData>
    fun findByMetadata(key: String, value: Any): Pair<Identifier, TData>?
    fun registryPopulated(mappings: NameIdRegistry, throwOnMissingId: Boolean = true) = Unit
    fun subscribe(reference: RegistryReference<TData>, handler: (TData?) -> Unit)
    fun unsubscribe(reference: RegistryReference<TData>, handler: (TData?) -> Unit)
    
    /**
     * Registers a reload listener to receive notifications about registry-wide changes.
     */
    fun addReloadListener(listener: RegistryReloadListener<TData>)
    
    /**
     * Removes a previously registered reload listener.
     */
    fun removeReloadListener(listener: RegistryReloadListener<TData>)
}