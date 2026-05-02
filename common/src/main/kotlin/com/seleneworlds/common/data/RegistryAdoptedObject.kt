package com.seleneworlds.common.data

abstract class RegistryAdoptedObject<T : Any> : RegistryObject<T>() {
    override lateinit var identifier: Identifier

    override lateinit var registry: Registry<T>
}
