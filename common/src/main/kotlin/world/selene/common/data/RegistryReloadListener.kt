package world.selene.common.data

/**
 * Interface for listening to registry-wide reload events.
 * Implementations can register with specific registries to be notified
 * when registry objects change due to loading, updating, or removal.
 */
interface RegistryReloadListener<TData : Any> {
    /**
     * Called when the registry is completely reloaded (all entries cleared and reloaded).
     * This typically happens during initial load or full bundle reloads.
     */
    fun onRegistryReloaded(registry: Registry<TData>) = Unit

    /**
     * Called when a single entry is added or updated in the registry.
     * 
     * @param registry The registry that changed
     * @param identifier The identifier of the changed entry
     * @param oldData The previous data
     * @param newData The new data
     */
    fun onEntryChanged(registry: Registry<TData>, identifier: Identifier, oldData: TData, newData: TData) = Unit

    /**
     * Called when an entry is removed from the registry.
     * This is a convenience method that calls onEntryChanged with newData = null.
     */
    fun onEntryRemoved(registry: Registry<TData>, identifier: Identifier, oldData: TData?) = Unit

    /**
     * Called when a new entry is added to the registry.
     * This is a convenience method that calls onEntryChanged with oldData = null.
     */
    fun onEntryAdded(registry: Registry<TData>, identifier: Identifier, newData: TData?) = Unit
}
