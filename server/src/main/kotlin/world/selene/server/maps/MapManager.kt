package world.selene.server.maps

import world.selene.common.data.NameIdRegistry
import world.selene.common.data.TileRegistry

class MapManager(private val tileRegistry: TileRegistry, private val nameIdRegistry: NameIdRegistry) {
    fun createMapTree(): MapTree {
        return MapTree(tileRegistry, nameIdRegistry)
    }
}