package world.selene.server.attributes.filters

import world.selene.server.attributes.Attribute

interface AttributeFilter<T : Any?> {
    val enabled: Boolean
    fun apply(attribute: Attribute<T>, value: T): T
}