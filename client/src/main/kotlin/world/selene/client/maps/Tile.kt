package world.selene.client.maps

import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.utils.Pool
import party.iroiro.luajava.Lua
import world.selene.client.grid.ClientGrid
import world.selene.client.rendering.visual2d.iso.IsoVisual
import world.selene.client.scene.Renderable
import world.selene.client.rendering.environment.Environment
import world.selene.client.rendering.visual.VisualCreationContext
import world.selene.client.rendering.visual.VisualManager
import world.selene.client.scene.Scene
import world.selene.common.data.TileDefinition
import world.selene.common.lua.LuaMappedMetatable
import world.selene.common.lua.LuaMetatable
import world.selene.common.lua.LuaMetatableProvider
import world.selene.common.util.Coordinate

class Tile(private val grid: ClientGrid, private val visualManager: VisualManager, private val pool: TilePool) :
    Pool.Poolable, Renderable,
    LuaMetatableProvider {
    lateinit var tileDefinition: TileDefinition
    var visual: IsoVisual? = null

    override val sortLayerOffset: Int get() = visual?.sortLayerOffset ?: 0
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
        visual?.update(delta)
        if (currentOcclusionAlpha != targetOcclusionAlpha) {
            val diff = targetOcclusionAlpha - currentOcclusionAlpha
            val step = fadeSpeed * delta
            currentOcclusionAlpha = when {
                kotlin.math.abs(diff) <= step -> targetOcclusionAlpha
                diff > 0 -> currentOcclusionAlpha + step
                else -> currentOcclusionAlpha - step
            }
        }
    }

    private val tmpRenderBounds = Rectangle()
    override fun render(batch: Batch, environment: Environment) {
        visual?.let {
            val displayX = grid.getScreenX(coordinate)
            val displayY = grid.getScreenY(coordinate) + environment.getSurfaceOffset(coordinate)
            val bounds = getBounds(displayX, displayY, tmpRenderBounds)
            if (environment.shouldRender(coordinate, bounds)) {
                val occluding = environment.occludesFocus(coordinate, bounds)
                targetOcclusionAlpha = if (occluding) 0.3f else 1f
                batch.color.set(environment.getColor(coordinate))
                batch.color = batch.color.mul(1f, 1f, 1f, currentOcclusionAlpha)
                it.render(batch, displayX, displayY)
                environment.applySurfaceOffset(coordinate, it.surfaceHeight)
            }
        }
    }

    fun getBounds(x: Float, y: Float, outRect: Rectangle): Rectangle {
        return visual?.getBounds(x, y, outRect) ?: outRect.set(x, y, 0f, 0f)
    }

    override fun reset() {
        localSortLayer = 0
        coordinate = Coordinate.Zero
        visual = null
        currentOcclusionAlpha = 1f
        targetOcclusionAlpha = 1f
    }

    override fun luaMetatable(lua: Lua): LuaMetatable {
        return luaMeta
    }

    fun updateVisual() {
        visual = visualManager.createVisual(tileDefinition.visual, VisualCreationContext(coordinate)) as? IsoVisual
    }

    override fun addedToScene(scene: Scene) {
    }

    override fun removedFromScene(scene: Scene) {
        pool.free(this)
    }

    companion object {
        val luaMeta = LuaMappedMetatable(Tile::class) {
            readOnly(Tile::coordinate)
            readOnly(Tile::tileDefinition, "Definition")
            readOnly(Tile::visual)
            readOnly(Tile::x)
            readOnly(Tile::y)
            readOnly(Tile::z)
            getter("Name") { lua ->
                val tile = lua.checkSelf()
                lua.push(tile.tileDefinition.name)
                1
            }
        }
    }
}
