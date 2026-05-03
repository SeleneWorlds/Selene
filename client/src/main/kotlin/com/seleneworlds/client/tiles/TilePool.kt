package com.seleneworlds.client.tiles

import com.badlogic.gdx.utils.Pool
import org.koin.mp.KoinPlatform.getKoin

class TilePool : Pool<Tile>() {
    // TODO While convenient like this, I think we should instead inject into TilePool instead and pass services manually to Tile, to avoid the Koin overhead during pool population.
    override fun newObject(): Tile = getKoin().get(Tile::class)
}