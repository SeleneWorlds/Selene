package world.selene.common.data

sealed interface RegistryReference<T : Any> {

    val valid get() = get() != null

    val identifier: Identifier

    fun get(): T?

    fun subscribe(handler: (T) -> Unit)

    class ByIdentifier<T : Any>(val registry: Registry<T>, override val identifier: Identifier) : RegistryReference<T> {
        private var cache: T? = null

        init {
            subscribe {
                cache = it
            }
        }

        private fun resolve(): T? {
            return registry.get(identifier)
        }

        override fun get(): T? {
            if (cache == null) {
                cache = resolve()
            }
            return cache
        }

        override fun subscribe(handler: (T) -> Unit) {
            registry.subscribe(this, handler)
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
    }

    companion object {
        @Suppress("UNCHECKED_CAST")
        fun <T : Any> unbound(): RegistryReference<T> {
            return Unbound as RegistryReference<T>
        }
    }
}
