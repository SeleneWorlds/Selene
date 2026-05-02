package com.seleneworlds.server.tiles

import com.seleneworlds.common.data.RegistryReference
import com.seleneworlds.common.grid.Coordinate
import com.seleneworlds.common.script.ExposedApi
import com.seleneworlds.common.tiles.TileDefinition
import com.seleneworlds.server.dimensions.Dimension

class TransientTile(
    val definition: RegistryReference<TileDefinition>,
    val dimension: Dimension,
    val coordinate: Coordinate
) : ExposedApi<TransientTileApi> {
    override val api = TransientTileApi(this)
    val identifier get() = definition.identifier
    val x get() = coordinate.x
    val y get() = coordinate.y
    val z get() = coordinate.z
}
