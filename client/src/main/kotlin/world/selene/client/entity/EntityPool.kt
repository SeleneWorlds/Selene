package world.selene.client.entity

import com.badlogic.gdx.utils.Pool
import org.koin.mp.KoinPlatform.getKoin

class EntityPool() : Pool<Entity>() {
    override fun newObject(): Entity = getKoin().get(Entity::class)
}
