package world.selene.common.data

interface InstancedComponent<T : ConfiguredComponent> {
    fun instantiate(): T
}