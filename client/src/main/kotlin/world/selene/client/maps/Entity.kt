package world.selene.client.maps

import com.badlogic.gdx.Gdx
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
import world.selene.common.data.ClientScriptComponent
import world.selene.common.data.ConfiguredComponent
import world.selene.common.data.VisualComponent
import world.selene.common.data.EntityDefinition
import world.selene.common.data.InstancedComponent
import world.selene.common.lua.LuaManager
import world.selene.common.lua.LuaProxyProvider
import world.selene.common.lua.checkInt
import world.selene.common.lua.checkJavaObject
import world.selene.common.lua.checkString
import world.selene.common.lua.newTable
import world.selene.common.util.Coordinate
import kotlin.collections.forEach
import kotlin.math.max

class Entity(
    val luaManager: LuaManager,
    val objectMapper: ObjectMapper,
    val visualManager: VisualManager,
    val scene: Scene,
    val grid: ClientGrid
) :
    Pool.Poolable, Renderable {
    var networkId: Int = 0
    var entityName: String? = null
    var entityDefinition: EntityDefinition? = null
    var components: MutableMap<String, ConfiguredComponent> = mutableMapOf()
    val motionQueue: ArrayDeque<EntityMotion> = ArrayDeque()
    val luaProxy = EntityLuaProxy(this)

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
        components.values.forEach { script ->
            if (script is ClientScriptComponent) {
                val lua = luaManager.lua
                if (script.data == null) {
                    script.data = lua.newTable {}
                }
                if (script.module == null) {
                    script.module = luaManager.requireModule(script.script).also {
                        lua.push(it)
                    }
                    lua.getField(-1, "Initialize")
                    if (lua.isFunction(-1)) {
                        lua.push(luaProxy, Lua.Conversion.NONE)
                        lua.push(script.data!!)
                        lua.pCall(2, 0)
                    }
                    lua.pop(1)
                }
                lua.push(script.module!!)
                lua.getField(-1, "TickEntity")
                if (lua.isFunction(-1)) {
                    lua.push(luaProxy, Lua.Conversion.NONE)
                    lua.push(script.data!!)
                    lua.push(Gdx.graphics.deltaTime)
                    lua.pCall(3, 0)
                }
                lua.pop(1)
            }
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
                context.color.set(component.r, component.g, component.b, component.a)
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
                visualManager.buildInstance(component.visual, component.properties)?.let {
                    if (it is AnimatorVisualInstance) {
                        it.animator = HumanoidAnimator(this)
                    }
                    visualInstances.add(ComponentVisualInstance(name, component, it))
                }
            }
        }
    }

    fun setCoordinate(coordinate: Coordinate) {
        this.coordinate = coordinate
        position.x = coordinate.x.toFloat()
        position.y = coordinate.y.toFloat()
        position.z = coordinate.z.toFloat()
        scene.updateSorting(this)
    }

    fun addComponent(name: String, component: ConfiguredComponent) {
        this.components[name] = component
    }

    fun setupComponents(overrides: Map<String, ConfiguredComponent>) {
        entityDefinition?.components?.forEach {
            this.components[it.key] = it.value
        }
        overrides.forEach {
            this.components[it.key] = it.value
        }
        this.components.forEach { (key, value) ->
            if (value is InstancedComponent<*>) {
                this.components[key] = value.instantiate()
            }
        }
    }

    class EntityLuaProxy(private val delegate: Entity) {
        val Coordinate get() = delegate.coordinate

        fun SetCoordinate(lua: Lua): Int {
            val coordinate: Coordinate = if (lua.isNumber(2)) {
                Coordinate(lua.checkInt(2), lua.checkInt(3), lua.checkInt(4))
            } else {
                lua.checkJavaObject(2, Coordinate::class)
            }
            delegate.setCoordinate(coordinate)
            return 0
        }

        fun AddComponent(lua: Lua): Int {
            val componentName = lua.checkString(2)
            val componentData = lua.toMap(3)
            val component = delegate.objectMapper.convertValue(componentData, ConfiguredComponent::class.java)
            delegate.addComponent(componentName, component)
            return 0
        }

        fun GetComponent(lua: Lua): Int {
            val componentName = lua.checkString(2)
            val component = delegate.components[componentName]
            lua.push(if (component is LuaProxyProvider<*>) component.luaProxy else component, Lua.Conversion.NONE)
            return 1
        }

        fun GetVisual(lua: Lua): Int {
            val componentName = lua.checkString(2)
            val visualInstance =
                delegate.visualInstances.firstOrNull { it.componentName == componentName }?.visualInstance
            lua.push(visualInstance, Lua.Conversion.NONE)
            return 1
        }

        fun UpdateVisual() {
            delegate.updateVisual()
        }

        fun Spawn() {
            // TODO should be added to entitiesByCoordinate too
            delegate.updateVisual()
            delegate.scene.add(delegate)
        }

        fun Despawn() {
            // TODO should be removed from entitiesByCoordinate too
            delegate.scene.remove(delegate)
        }
    }
}
