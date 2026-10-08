package com.seleneworlds.common.data

interface RegistryProvider {
    @Deprecated("Use getRegistry(identifier) instead", ReplaceWith("getRegistry(Identifier.parse(name))"))
    fun getRegistry(name: String): Registry<*>? = getRegistry(Identifier.parse(name))

    fun getRegistries(): Collection<Registry<*>>

    fun getRegistry(identifier: Identifier): Registry<*>?
}
