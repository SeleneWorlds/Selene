package world.selene.client.maps

import com.badlogic.gdx.utils.Pool
import org.koin.mp.KoinPlatform.getKoin
import world.selene.common.data.TileDefinition

class TilePool : Pool<Tile>() {
    override fun newObject(): Tile = getKoin().get(Tile::class)

    fun freeAll(tiles: List<Tile>) = tiles.forEach { free(it) }
}