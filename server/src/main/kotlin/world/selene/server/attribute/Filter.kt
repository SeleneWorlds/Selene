package world.selene.server.attribute

interface Filter<T : Any> {
    val name: String
    val enabled: Boolean
    fun apply(attribute: Attribute<*>, value: T?): T?
}