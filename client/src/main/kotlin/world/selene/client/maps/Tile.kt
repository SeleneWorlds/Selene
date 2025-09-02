package world.selene.client.maps

import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.utils.Pool
import party.iroiro.luajava.Lua
import world.selene.client.grid.ClientGrid
import world.selene.client.rendering.visual2d.iso.IsoVisual
import world.selene.client.rendering.SceneRenderer
import world.selene.client.scene.Renderable
import world.selene.client.old.VisualContext
import world.selene.client.rendering.visual.VisualCreationContext
import world.selene.client.rendering.visual.VisualManager
import world.selene.common.data.TileDefinition
import world.selene.common.lua.LuaMappedMetatable
import world.selene.common.lua.LuaMetatable
import world.selene.common.lua.LuaMetatableProvider
import world.selene.common.util.Coordinate

class Tile(private val grid: ClientGrid, private val visualManager: VisualManager) : Pool.Poolable, Renderable, LuaMetatableProvider {
    lateinit var tileDefinition: TileDefinition
    var visual: IsoVisual? = null

    override val sortLayerOffset: Int get() = visual?.sortLayerOffset ?: 0
    override val sortLayer: Int get() = grid.getSortLayer(coordinate, sortLayerOffset)
    override var localSortLayer: Int = 0
    override var coordinate: Coordinate = Coordinate.Zero

    val screenX get() = grid.getScreenX(coordinate)
    val screenY get() = grid.getScreenY(coordinate)

    val x get() = coordinate.x
    val y get() = coordinate.y
    val z get() = coordinate.z

    private var currentOcclusionAlpha: Float = 1f
    private var targetOcclusionAlpha: Float = 1f
    private val fadeSpeed: Float = 5f

    override fun update(delta: Float) {
        visual?.update(delta)
        // Smoothly approach targetOcclusionAlpha
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

    override fun render(sceneRenderer: SceneRenderer, spriteBatch: SpriteBatch, visualContext: VisualContext) {
        // val alpha = visualContext.interiorFadeAlpha
        // val occluded = visual.occludes(sceneRenderer, displayX, displayY, visualContext)
        // targetOcclusionAlpha = if (occluded) min(alpha, 0.3f) else alpha
        // visualContext.color.set(1f, 1f, 1f, currentOcclusionAlpha)
        visual?.let {
            it.render(spriteBatch, screenX, screenY - visualContext.offsetY)
            visualContext.offsetY += it.surfaceHeight
        }
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

    companion object {
        val luaMeta = LuaMappedMetatable(Tile::class) {
            readOnly(Tile::coordinate)
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
