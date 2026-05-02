package world.selene.client.tiles

import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.utils.Pool
import world.selene.client.data.Registries
import world.selene.client.grid.ClientGrid
import world.selene.client.rendering.environment.Environment
import world.selene.client.rendering.scene.Renderable
import world.selene.client.rendering.scene.Scene
import world.selene.client.rendering.visual.ReloadableVisual
import world.selene.client.rendering.visual.VisualCreationContext
import world.selene.client.rendering.visual.VisualFactory
import world.selene.common.data.RegistryReference
import world.selene.common.grid.Coordinate
import world.selene.common.script.ExposedApi
import world.selene.common.tiles.TileDefinition
import kotlin.math.abs

class Tile(
    private val grid: ClientGrid,
    private val registries: Registries,
    private val visualFactory: VisualFactory,
    private val pool: TilePool
) : Pool.Poolable, Renderable, ExposedApi<TileApi> {
    override val api = TileApi(this)
    var tileDefinition: RegistryReference<TileDefinition> = RegistryReference.unbound()
        set(value) {
            field.unsubscribeAll()
            field = value
            value.subscribe {
                updateVisual()
            }
        }
    var visual: ReloadableVisual = ReloadableVisual.None
        set(value) {
            if (field !== value) {
                field.dispose()
                field = value
            }
        }

    override val sortLayerOffset: Int get() = visual.sortLayerOffset
    override val sortLayer: Int get() = grid.getSortLayer(coordinate, sortLayerOffset)
    override var localSortLayer: Int = 0
    override var coordinate: Coordinate = Coordinate.Zero

    val x get() = coordinate.x
    val y get() = coordinate.y
    val z get() = coordinate.z

    private var currentOcclusionAlpha: Float = 1f
    private var targetOcclusionAlpha: Float = 1f
    private val fadeSpeed: Float = 5f

    override fun update(delta: Float) {
        visual.update(delta)
        if (currentOcclusionAlpha != targetOcclusionAlpha) {
            val diff = targetOcclusionAlpha - currentOcclusionAlpha
            val step = fadeSpeed * delta
            currentOcclusionAlpha = when {
                abs(diff) <= step -> targetOcclusionAlpha
                diff > 0 -> currentOcclusionAlpha + step
                else -> currentOcclusionAlpha - step
            }
        }
    }

    private val tmpRenderBounds = Rectangle()
    override fun render(batch: Batch, environment: Environment) {
        val displayX = grid.getScreenX(coordinate)
        val displayY = grid.getScreenY(coordinate) + environment.getSurfaceOffset(coordinate)
        val bounds = getBounds(displayX, displayY, tmpRenderBounds)
        if (environment.shouldRender(coordinate, bounds)) {
            val occluding = environment.occludesFocus(coordinate, bounds)
            targetOcclusionAlpha = if (occluding) 0.3f else 1f
            batch.color.set(environment.getColor(coordinate))
            batch.color = batch.color.mul(1f, 1f, 1f, currentOcclusionAlpha)
            visual.render(batch, displayX, displayY)
            environment.applySurfaceOffset(coordinate, visual.surfaceHeight)
        }
    }

    fun getBounds(x: Float, y: Float, outRect: Rectangle): Rectangle {
        return visual.getBounds(x, y, outRect)
    }

    override fun reset() {
        tileDefinition = RegistryReference.unbound()
        localSortLayer = 0
        coordinate = Coordinate.Zero
        visual = ReloadableVisual.None
        currentOcclusionAlpha = 1f
        targetOcclusionAlpha = 1f
    }

    fun updateVisual() {
        visual = tileDefinition.get()?.visual?.let {
            registries.visuals.getReference(it)
        }?.let {
            val context = VisualCreationContext(coordinate)
            ReloadableVisual.Instance(visualFactory, it, context)
        } ?: ReloadableVisual.None
    }

    override fun addedToScene(scene: Scene) {
    }

    override fun removedFromScene(scene: Scene) {
        pool.free(this)
    }
}
