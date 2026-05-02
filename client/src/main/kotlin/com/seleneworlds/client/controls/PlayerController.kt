package com.seleneworlds.client.controls

import com.seleneworlds.client.maps.ClientMap

class PlayerController(private val map: ClientMap) {
    var controlledEntityNetworkId: Int = -1
    val controlledEntity get() = map.getEntityByNetworkId(controlledEntityNetworkId)
}