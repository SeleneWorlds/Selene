package com.seleneworlds.server.maps

import com.seleneworlds.server.data.Registries
import com.seleneworlds.server.maps.tree.MapTree
import com.seleneworlds.server.maps.tree.MapTreeApi

class ServerMapApi(private val registries: Registries) {
    fun create(): MapTreeApi = MapTree(registries).api
}
