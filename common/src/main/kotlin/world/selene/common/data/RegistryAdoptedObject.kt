package world.selene.common.data

import com.fasterxml.jackson.annotation.JsonIgnore

abstract class RegistryAdoptedObject<T : Any> : RegistryObject<T>() {
    @get:JsonIgnore
    override lateinit var identifier: Identifier

    @get:JsonIgnore
    override lateinit var registry: Registry<T>
}