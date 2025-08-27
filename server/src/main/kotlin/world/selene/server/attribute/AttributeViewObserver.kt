package world.selene.server.attribute

interface AttributeViewObserver {
    fun attributeChanged(view: AttributeView, attributeKey: Any, attribute: Attribute<*>)
}