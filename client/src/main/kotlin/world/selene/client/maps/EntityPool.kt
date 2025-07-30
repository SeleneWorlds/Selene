package world.selene.client.maps

import com.badlogic.gdx.utils.Pool
import org.koin.mp.KoinPlatform.getKoin
import world.selene.client.visual.VisualManager
import world.selene.client.data.Registries
import world.selene.client.grid.Grid
import world.selene.client.scene.Scene

class EntityPool(private val registries: Registries) :
    Pool<Entity>() {
    override fun newObject(): Entity = getKoin().get(Entity::class)

    fun obtain(entityName: String): Entity {
        return super.obtain().apply {
            this.entityName = entityName
            this.entityDefinition = registries.entities.get(entityName)
        }
    }

    fun obtain(entityId: Int): Entity {
        return super.obtain().apply {
            this.entityName = registries.mappings.getName("entities", entityId)?.also {
                entityDefinition = registries.entities.get(it)
            }
        }
    }

    fun freeAll(entities: List<Entity>) = entities.forEach { free(it) }
}
