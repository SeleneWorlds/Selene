package world.selene.client.maps

import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.utils.Pool
import com.fasterxml.jackson.databind.ObjectMapper
import party.iroiro.luajava.Lua
import java.util.ArrayDeque
import world.selene.client.controls.EntityMotion
import world.selene.client.entity.component.EntityComponent
import world.selene.client.entity.component.EntityComponentFactory
import world.selene.client.grid.ClientGrid
import world.selene.client.entity.component.IsoComponent
import world.selene.client.entity.component.RenderableComponent
import world.selene.client.entity.component.TickableComponent
import world.selene.client.rendering.animator.HumanoidAnimatorController
import world.selene.client.scene.Renderable
import world.selene.client.scene.Scene
import world.selene.client.rendering.environment.Environment
import world.selene.common.data.ComponentConfiguration
import world.selene.common.data.EntityDefinition
import world.selene.common.lua.LuaMappedMetatable
import world.selene.common.lua.LuaMetatable
import world.selene.common.lua.LuaMetatableProvider
import world.selene.common.lua.checkCoordinate
import world.selene.common.lua.checkString
import world.selene.common.lua.checkUserdata
import world.selene.common.lua.toAnyMap
import world.selene.common.util.Coordinate
import kotlin.collections.forEach

class Entity(
    val objectMapper: ObjectMapper,
    val pool: EntityPool,
    val map: ClientMap,
    val grid: ClientGrid,
    val entityComponentFactory: EntityComponentFactory
) :
    Pool.Poolable, Renderable, LuaMetatableProvider {
    var networkId: Int = 0
    var entityDefinition: EntityDefinition? = null
        set(value) {
            field = value
            components.clear()
            tickableComponents.clear()
            renderableComponents.clear()
            value?.components?.forEach {
                addComponent(it.key, it.value)
            }
        }

    var scene: Scene? = null
    var removed: Boolean = false

    val components = mutableMapOf<String, EntityComponent>()
    val tickableComponents = mutableListOf<TickableComponent>()
    val renderableComponents = mutableListOf<RenderableComponent>()

    val lastRenderBounds = Rectangle()

    var processingComponents = false
    val componentsToBeAdded = mutableSetOf<EntityComponent>()
    val componentsToBeRemoved = mutableSetOf<EntityComponent>()

    val motionQueue: ArrayDeque<EntityMotion> = ArrayDeque()
    val animator = HumanoidAnimatorController(this)

    override var coordinate: Coordinate = Coordinate.Zero
        private set(value) {
            val prev = field
            field = value
            if (prev != value && !removed) {
                map.entityMoved(this, prev)
            }
        }

    var facing: Float = 0f
    val direction get() = grid.getDirection(facing)
    override var sortLayerOffset: Int = 0
    override val sortLayer: Int get() = grid.getSortLayer(position, sortLayerOffset)
    override var localSortLayer: Int = 0

    val position: Vector3 = Vector3(coordinate.x.toFloat(), coordinate.y.toFloat(), coordinate.z.toFloat())

    val screenX get() = grid.getScreenX(position)
    val screenY get() = grid.getScreenY(position)

    /**
     * Moves the entity to the target position over the given duration.
     * If skipQueue is true, clears the queue before adding the new motion.
     */
    fun move(target: Coordinate, duration: Float, facing: Float, skipQueue: Boolean = false) {
        this.facing = facing
        if (skipQueue) {
            motionQueue.clear()
        }
        val latestTarget = motionQueue.lastOrNull()?.end ?: this.coordinate
        if (latestTarget != target) {
            motionQueue.add(EntityMotion(latestTarget, target, duration))
        }
    }

    fun isInMotion(): Boolean {
        return motionQueue.isNotEmpty()
    }

    override fun update(delta: Float) {
        // Handle motion queue
        if (motionQueue.isNotEmpty()) {
            val motion = motionQueue.first()
            motion.timePassed += delta
            val t = (motion.timePassed / motion.duration).coerceAtMost(1f)
            // Interpolate smooth position
            position.x = motion.start.x + (motion.end.x - motion.start.x) * t
            position.y = motion.start.y + (motion.end.y - motion.start.y) * t
            position.z = motion.start.z + (motion.end.z - motion.start.z) * t
            // Only update grid coordinate once halfway
            if (t >= 0.5f) {
                coordinate = motion.end
            }
            if (motion.timePassed >= motion.duration) {
                // Snap to end and remove motion
                position.x = motion.end.x.toFloat()
                position.y = motion.end.y.toFloat()
                position.z = motion.end.z.toFloat()
                motionQueue.removeFirst()
            }
            scene?.updateSorting(this)
        }

        processComponents {
            tickableComponents.forEach { component ->
                component.update(this, delta)
            }
        }
    }

    private inline fun processComponents(runnable: () -> Unit) {
        processingComponents = true
        runnable()
        processingComponents = false
        for (component in componentsToBeAdded) {
            processAddedComponent(component)
        }
        componentsToBeAdded.clear()
        for (component in componentsToBeRemoved) {
            processRemovedComponent(component)
        }
        componentsToBeRemoved.clear()
    }

    private val tmpDisplayPos = Vector2()
    private val tmpRenderRectangle = Rectangle()
    override fun render(batch: Batch, environment: Environment) {
        lastRenderBounds.set(0f, 0f, 0f, 0f)
        processComponents {
            renderableComponents.forEach { component ->
                tmpDisplayPos.set(screenX, screenY + environment.getSurfaceOffset(coordinate))
                component.positioner.applyPositioning(this, tmpDisplayPos)
                batch.color.set(environment.getColor(coordinate))
                component.render(this, batch, tmpDisplayPos.x, tmpDisplayPos.y)
                if (component is IsoComponent) {
                    environment.applySurfaceOffset(coordinate, component.surfaceHeight)
                }
                component.getBounds(tmpDisplayPos.x, tmpDisplayPos.y, tmpRenderRectangle)
                if (lastRenderBounds.width == 0f) {
                    lastRenderBounds.set(tmpRenderRectangle)
                } else {
                    lastRenderBounds.merge(tmpRenderRectangle)
                }
            }
        }
    }

    override fun reset() {
        networkId = 0
        motionQueue.clear()
        coordinate = Coordinate.Zero
        facing = 0f
        localSortLayer = 0
        entityDefinition = null
        processingComponents = false
        removed = false
        componentsToBeAdded.clear()
        componentsToBeRemoved.clear()
        position.set(0f, 0f, 0f)
    }

    fun setCoordinateAndUpdate(coordinate: Coordinate) {
        if (this.coordinate != coordinate) {
            this.coordinate = coordinate
            position.x = coordinate.x.toFloat()
            position.y = coordinate.y.toFloat()
            position.z = coordinate.z.toFloat()
            scene?.updateSorting(this)
        }
    }

    fun addComponent(name: String, componentConfiguration: ComponentConfiguration) {
        val component = entityComponentFactory.create(this, componentConfiguration)
        if (component != null) {
            addComponent(name, component)
        }
    }

    fun addComponent(name: String, component: EntityComponent) {
        val prev = components.put(name, component)
        if (processingComponents) {
            prev?.let {
                componentsToBeAdded.remove(it)
                componentsToBeRemoved.add(it)
            }
            componentsToBeRemoved.remove(component)
            componentsToBeAdded.add(component)
        } else {
            prev?.let { processRemovedComponent(it) }
            processAddedComponent(component)
        }
    }

    private fun computeSortLayerOffset(): Int {
        return renderableComponents.asSequence().mapNotNull { it as? IsoComponent }.maxOfOrNull { it.sortLayerOffset }
            ?: 0
    }

    private fun processAddedComponent(component: EntityComponent) {
        if (component is TickableComponent) {
            tickableComponents.add(component)
        }
        if (component is RenderableComponent) {
            renderableComponents.add(component)
        }
        val prevSortLayerOffset = sortLayerOffset
        sortLayerOffset = computeSortLayerOffset()
        if (prevSortLayerOffset != sortLayerOffset) {
            scene?.updateSorting(this)
        }
    }

    private fun processRemovedComponent(component: EntityComponent) {
        if (component is TickableComponent) {
            tickableComponents.remove(component)
        }
        if (component is RenderableComponent) {
            renderableComponents.remove(component)
        }
    }

    private fun spawn() {
        removed = false
        map.addEntity(this)
    }

    private fun despawn() {
        if (!removed) {
            map.removeEntity(this)
            removed = true
        }
    }

    fun hasTag(tag: String): Boolean {
        return entityDefinition?.tags?.contains(tag) ?: false
    }

    override fun addedToScene(scene: Scene) {
        this.scene = scene
    }

    override fun removedFromScene(scene: Scene) {
        if (this.scene != null) {
            pool.free(this)
        }
        this.scene = null
    }

    override fun luaMetatable(lua: Lua): LuaMetatable {
        return luaMeta
    }

    fun turnTo(facing: Float) {
        this.facing = facing
    }

    companion object {
        private fun luaSpawn(lua: Lua): Int {
            val self = lua.checkUserdata<Entity>(1)
            self.spawn()
            return 0
        }

        private fun luaDespawn(lua: Lua): Int {
            val self = lua.checkUserdata<Entity>(1)
            self.despawn()
            return 0
        }

        private fun luaSetCoordinate(lua: Lua): Int {
            val self = lua.checkUserdata<Entity>(1)
            val (coordinate, _) = lua.checkCoordinate(2)
            self.setCoordinateAndUpdate(coordinate)
            return 0
        }

        private fun luaAddComponent(lua: Lua): Int {
            val self = lua.checkUserdata<Entity>(1)
            val componentName = lua.checkString(2)
            val componentData = lua.toAnyMap(3)
            val componentConfiguration =
                self.objectMapper.convertValue(componentData, ComponentConfiguration::class.java)
            self.addComponent(componentName, componentConfiguration)
            return 0
        }

        private fun luaGetComponent(lua: Lua): Int {
            val self = lua.checkUserdata<Entity>(1)
            val componentName = lua.checkString(2)
            val component = self.components[componentName]
            lua.push(component, Lua.Conversion.NONE)
            return 1
        }

        val luaMeta = LuaMappedMetatable(Entity::class) {
            readOnly(Entity::coordinate)
            callable(::luaSpawn)
            callable(::luaDespawn)
            callable(::luaSetCoordinate)
            callable(::luaAddComponent)
            callable(::luaGetComponent)
        }
    }
}
