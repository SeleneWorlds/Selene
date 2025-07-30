package world.selene.client.maps

import com.badlogic.gdx.utils.Pool
import world.selene.client.data.Registries
import world.selene.client.grid.Grid
import world.selene.client.visual.VisualManager

class TilePool(private val registries: Registries, private val visualManager: VisualManager, private val grid: Grid) : Pool<Tile>() {
    override fun newObject(): Tile = Tile(grid)
    fun obtain(tileId: Int): Tile {
        return super.obtain().apply {
            tileName = registries.mappings.getName("tiles", tileId)?.also {
                tileDefinition = registries.tiles.get(it)
            }
            visualInstance = visualManager.getVisualInstance(this)
        }
    }
    fun freeAll(tiles: List<Tile>) = tiles.forEach { free(it) }
}