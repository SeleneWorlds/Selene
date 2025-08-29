package world.selene.client.maps

import com.badlogic.gdx.utils.Pool
import world.selene.client.grid.ClientGrid
import world.selene.client.visual.VisualManager
import world.selene.common.data.TileDefinition

class TilePool(private val visualManager: VisualManager, private val grid: ClientGrid) : Pool<Tile>() {
    override fun newObject(): Tile = Tile(grid)

    fun obtain(tileDefinition: TileDefinition): Tile {
        return super.obtain().apply {
            this.tileDefinition = tileDefinition
            visualInstance = visualManager.getVisualInstance(this)
        }
    }

    fun freeAll(tiles: List<Tile>) = tiles.forEach { free(it) }
}