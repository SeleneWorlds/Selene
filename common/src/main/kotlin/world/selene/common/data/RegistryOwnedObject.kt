package world.selene.common.data

interface RegistryOwnedObject<T : Any> {
    val registry: Registry<T>
    val identifier: Identifier

    val asReference get() = RegistryReference.ByIdentifier(registry, identifier)
}