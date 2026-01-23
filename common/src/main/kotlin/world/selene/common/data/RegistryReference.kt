package world.selene.common.data

sealed interface RegistryReference<T : Any> {

    val valid get() = get() != null

    val identifier: Identifier

    fun get(): T?

    fun subscribe(handler: (T) -> Unit)

    fun unsubscribeAll()

    class ByIdentifier<T : Any>(val registry: Registry<T>, override val identifier: Identifier) : RegistryReference<T> {
        private var cache: T? = null
        private var lastCacheKey: Long = -1
        private val subscriptions = mutableListOf<(T) -> Unit>()

        private fun resolve(): T? {
            return registry.get(identifier)
        }

        override fun get(): T? {
            if (registry is CacheableRegistry) {
                val currentCacheKey = registry.cacheKey
                if (cache == null || lastCacheKey != currentCacheKey) {
                    cache = resolve()
                    lastCacheKey = currentCacheKey
                }
                return cache
            }
            return resolve()
        }

        override fun subscribe(handler: (T) -> Unit) {
            subscriptions.add(handler)
            registry.subscribe(this, handler)
        }

        override fun unsubscribeAll() {
            subscriptions.forEach { handler ->
                registry.unsubscribe(this, handler)
            }
            subscriptions.clear()
        }
    }

    object Unbound : RegistryReference<Any> {
        override val valid: Boolean = false

        override val identifier: Identifier = Identifier.withDefaultNamespace("unbound")

        override fun get(): Any? {
            return null
        }

        override fun subscribe(handler: (Any) -> Unit) {
        }

        override fun unsubscribeAll() {
        }
    }

    companion object {
        @Suppress("UNCHECKED_CAST")
        fun <T : Any> unbound(): RegistryReference<T> {
            return Unbound as RegistryReference<T>
        }
    }
}
