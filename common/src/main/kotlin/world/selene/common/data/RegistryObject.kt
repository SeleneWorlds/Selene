package world.selene.common.data

import com.fasterxml.jackson.annotation.JsonIgnore

abstract class RegistryObject<T : Any> : RegistryOwnedObject<T>, IdMappedObject {

    @get:JsonIgnore
    override var id: Int = -1
}
