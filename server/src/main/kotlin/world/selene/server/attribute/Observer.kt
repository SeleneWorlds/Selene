package world.selene.server.attribute

interface Observer {
    fun attributeChanged(attribute: Attribute<*>, observableData: Any?)
}