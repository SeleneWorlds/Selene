package world.selene.common.data

interface RegistryOwnedObject<T : Any> {
    var registry: Registry<T>
    var identifier: Identifier
}