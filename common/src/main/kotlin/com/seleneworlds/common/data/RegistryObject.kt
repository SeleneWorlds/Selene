package com.seleneworlds.common.data

abstract class RegistryObject<T : Any> : RegistryOwnedObject<T>, IdMappedObject {
    override var id: Int = -1
}
