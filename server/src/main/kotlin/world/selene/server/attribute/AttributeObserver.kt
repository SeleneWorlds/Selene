package world.selene.server.attribute

interface AttributeObserver {
    fun attributeChanged(attribute: Attribute<*>, observableData: Any?)
}