package world.selene.server.attribute

interface AttributeFilter<T : Any?> {
    val enabled: Boolean
    fun apply(attribute: Attribute<T>, value: T): T
}