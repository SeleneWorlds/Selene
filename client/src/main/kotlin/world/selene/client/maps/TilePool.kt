package world.selene.client.maps

import com.badlogic.gdx.utils.Pool
import org.koin.mp.KoinPlatform.getKoin

class TilePool : Pool<Tile>() {
    override fun newObject(): Tile = getKoin().get(Tile::class)
}