package world.selene.server.tiles

import world.selene.common.data.RegistryReference
import world.selene.common.grid.Coordinate
import world.selene.common.tiles.TileDefinition
import world.selene.server.dimensions.Dimension

class TransientTile(
    val definition: RegistryReference<TileDefinition>,
    val dimension: Dimension,
    val coordinate: Coordinate
) {
    val api = TransientTileApi(this)
    val identifier get() = definition.identifier
    val x get() = coordinate.x
    val y get() = coordinate.y
    val z get() = coordinate.z
}
