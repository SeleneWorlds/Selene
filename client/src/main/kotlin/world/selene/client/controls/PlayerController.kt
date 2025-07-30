package world.selene.client.controls

import world.selene.client.maps.ClientMap

class PlayerController(private val map: ClientMap) {
    var controlledEntityNetworkId: Int = -1
    val controlledEntity get() = map.getEntityByNetworkId(controlledEntityNetworkId)
}