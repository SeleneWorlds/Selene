package world.selene.client.maps

import com.badlogic.gdx.utils.Pool
import org.koin.mp.KoinPlatform.getKoin
import world.selene.common.data.EntityDefinition

class EntityPool() :
    Pool<Entity>() {
    override fun newObject(): Entity = getKoin().get(Entity::class)

    fun obtain(entityDefinition: EntityDefinition): Entity {
        return super.obtain().apply {
            this.entityDefinition = entityDefinition
        }
    }
}
