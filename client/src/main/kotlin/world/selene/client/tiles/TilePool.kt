package world.selene.client.tiles

import com.badlogic.gdx.utils.Pool
import org.koin.mp.KoinPlatform.getKoin

class TilePool : Pool<Tile>() {
    override fun newObject(): Tile = getKoin().get(Tile::class)
}