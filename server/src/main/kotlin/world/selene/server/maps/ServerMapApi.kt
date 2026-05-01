package world.selene.server.maps

import world.selene.server.data.Registries
import world.selene.server.maps.tree.MapTree
import world.selene.server.maps.tree.MapTreeApi

class ServerMapApi(private val registries: Registries) {
    fun create(): MapTreeApi = MapTree(registries).api
}
