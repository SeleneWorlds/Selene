package world.selene.server.attribute

interface Filter<T : Any> {
    fun apply(attribute: Attribute<*>, value: T?): T?
}