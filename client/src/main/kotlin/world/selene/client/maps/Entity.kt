package world.selene.client.maps

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.utils.Pool
import com.fasterxml.jackson.databind.ObjectMapper
import party.iroiro.luajava.Lua
import world.selene.client.animator.HumanoidAnimator
import java.util.ArrayDeque
import world.selene.client.controls.EntityMotion
import world.selene.client.grid.ClientGrid
import world.selene.client.rendering.SceneRenderer
import world.selene.client.scene.Renderable
import world.selene.client.scene.Scene
import world.selene.client.visual.AnimatorVisualInstance
import world.selene.client.visual.SizedVisualInstance
import world.selene.client.visual.VisualContext
import world.selene.client.visual.VisualInstance
import world.selene.client.visual.VisualManager
import world.selene.common.data.ComponentConfiguration
import world.selene.common.data.EntityDefinition
import world.selene.common.lua.LuaManager
import world.selene.common.lua.LuaMappedMetatable
import world.selene.common.lua.LuaMetatable
import world.selene.common.lua.LuaMetatableProvider
import world.selene.common.lua.checkCoordinate
import world.selene.common.lua.checkString
import world.selene.common.util.Coordinate
import kotlin.collections.forEach
import kotlin.math.max

class Entity(
    val luaManager: LuaManager,
    val objectMapper: ObjectMapper,
    val visualManager: VisualManager,
    val scene: Scene,
    val map: ClientMap,
    val grid: ClientGrid
) :
    Pool.Poolable, Renderable, LuaMetatableProvider {
    var networkId: Int = 0
    var entityName: String? = null
    var entityDefinition: EntityDefinition? = null
    var components: MutableMap<String, EntityComponent> = mutableMapOf()
    val motionQueue: ArrayDeque<EntityMotion> = ArrayDeque()

    override var coordinate: Coordinate = Coordinate.Zero; private set
    var facing: Float = 0f
    val direction get() = grid.getDirection(facing)
    override val sortLayerOffset: Int get() = visualInstances.maxOfOrNull { it.visualInstance.sortLayerOffset } ?: 0
    override val sortLayer: Int get() = grid.getSortLayer(position, sortLayerOffset)
    override var localSortLayer: Int = 0

    data class ComponentVisualInstance(
        val componentName: String,
        val component: VisualComponent,
        val visualInstance: VisualInstance
    )

    val visualInstances = mutableListOf<ComponentVisualInstance>()
    val mainVisualInstance get() = visualInstances.firstOrNull()?.visualInstance

    var position: Vector3 = Vector3(coordinate.x.toFloat(), coordinate.y.toFloat(), coordinate.z.toFloat())

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
            scene.updateSorting(this)
        }
        visualInstances.forEach { it.visualInstance.update(delta) }
    }

    override fun render(sceneRenderer: SceneRenderer, spriteBatch: SpriteBatch, visualContext: VisualContext) {
        components.values.forEach { component ->
            component.update(this)
        }

        val displayX = screenX
        val displayY = screenY
        val context = visualContext.copy(maxWidth = 0f, maxHeight = 0f, color = Color.WHITE.cpy())
        visualInstances.forEach { (_, component, visualInstance) ->
            if (visualInstance is SizedVisualInstance) {
                context.maxWidth = max(context.maxWidth, visualInstance.width)
                context.maxHeight = max(context.maxHeight, visualInstance.height)
            }
            if (visualInstance.shouldRender(sceneRenderer, displayX, displayY, context)) {
                context.color.set(component.red, component.green, component.blue, component.alpha)
                visualInstance.render(spriteBatch, displayX, displayY, context)
            }
        }
    }

    override fun reset() {
        networkId = 0
        coordinate = Coordinate.Zero
        visualInstances.clear()
    }

    fun updateVisual() {
        visualInstances.clear()
        components.entries.forEach { (name, component) ->
            if (component is VisualComponent) {
                visualManager.buildInstance(component.configuration.visual, component.configuration.properties)?.let {
                    if (it is AnimatorVisualInstance) {
                        it.animator = HumanoidAnimator(this)
                    }
                    visualInstances.add(ComponentVisualInstance(name, component, it))
                }
            }
        }
    }

    fun setCoordinate(coordinate: Coordinate) {
        if (this.coordinate == coordinate) {
            return
        }

        this.coordinate = coordinate
        position.x = coordinate.x.toFloat()
        position.y = coordinate.y.toFloat()
        position.z = coordinate.z.toFloat()
        scene.updateSorting(this)
    }

    fun setupComponents(overrides: Map<String, ComponentConfiguration>) {
        entityDefinition?.components?.forEach {
            this.components[it.key] = it.value.create()
        }
        overrides.forEach {
            this.components[it.key] = it.value.create()
        }
    }

    private fun spawn() {
        if (visualInstances.isEmpty()) {
            updateVisual()
        }
        map.addEntity(this)
    }

    private fun despawn() {
        map.removeEntity(this)
    }

    val luaMeta = LuaMappedMetatable(this) {
        readOnly(::coordinate)
        callable(::updateVisual)
        callable(::spawn)
        callable(::despawn)
        callable("SetCoordinate") {
            val (coordinate, _) = it.checkCoordinate(2)
            setCoordinate(coordinate)
            0
        }
        callable("AddComponent") {
            val componentName = it.checkString(2)
            val componentData = it.toMap(3)
            val componentConfiguration = objectMapper.convertValue(componentData, ComponentConfiguration::class.java)
            components[componentName] = componentConfiguration.create()
            0
        }
        callable("GetComponent") {
            val componentName = it.checkString(2)
            val component = components[componentName]
            it.push(component, Lua.Conversion.NONE)
            1
        }
        callable("GetVisual") { lua ->
            val componentName = lua.checkString(2)
            val visualInstance = visualInstances.firstOrNull { it.componentName == componentName }?.visualInstance
            lua.push(visualInstance, Lua.Conversion.NONE)
            1
        }
    }

    override fun luaMetatable(lua: Lua): LuaMetatable {
        return luaMeta
    }

    fun hasTag(tag: String): Boolean {
        return entityDefinition?.tags?.contains(tag) ?: false
    }
}
