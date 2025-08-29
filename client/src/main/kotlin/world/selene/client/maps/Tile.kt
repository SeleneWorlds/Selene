package world.selene.client.maps

import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.utils.Pool
import party.iroiro.luajava.Lua
import world.selene.client.grid.ClientGrid
import world.selene.client.rendering.SceneRenderer
import world.selene.client.scene.Renderable
import world.selene.client.visual.VisualContext
import world.selene.client.visual.VisualInstance
import world.selene.common.data.TileDefinition
import world.selene.common.lua.LuaMappedMetatable
import world.selene.common.lua.LuaMetatable
import world.selene.common.lua.LuaMetatableProvider
import world.selene.common.util.Coordinate
import kotlin.math.min

class Tile(private val grid: ClientGrid) : Pool.Poolable, Renderable, LuaMetatableProvider {
    lateinit var tileDefinition: TileDefinition
    val visual get() = visualInstance

    override val sortLayerOffset: Int get() = visualInstance?.sortLayerOffset ?: 0
    override val sortLayer: Int get() = grid.getSortLayer(coordinate, sortLayerOffset)
    override var localSortLayer: Int = 0
    override var coordinate: Coordinate = Coordinate.Zero
    var visualInstance: VisualInstance? = null
    
    val screenX get() = grid.getScreenX(coordinate)
    val screenY get() = grid.getScreenY(coordinate)

    val x get() = coordinate.x
    val y get() = coordinate.y
    val z get() = coordinate.z

    private var currentOcclusionAlpha: Float = 1f
    private var targetOcclusionAlpha: Float = 1f
    private val fadeSpeed: Float = 5f

    override fun update(delta: Float) {
        visualInstance?.update(delta)
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
        val displayX = screenX
        val displayY = screenY
        visualInstance?.let { visualInstance ->
            if (visualInstance.shouldRender(sceneRenderer, displayX, displayY, visualContext)) {
                val alpha = visualContext.interiorFadeAlpha
                val occluded = visualInstance.occludes(sceneRenderer, displayX, displayY, visualContext)
                targetOcclusionAlpha = if (occluded) min(alpha, 0.3f) else alpha
                visualContext.color.set(1f, 1f, 1f, currentOcclusionAlpha)
                visualInstance.render(spriteBatch, displayX, displayY, visualContext)
            }
        }
    }

    override fun reset() {
        localSortLayer = 0
        coordinate = Coordinate.Zero
        visualInstance = null
        currentOcclusionAlpha = 1f
        targetOcclusionAlpha = 1f
    }

    override fun luaMetatable(lua: Lua): LuaMetatable {
        return luaMeta
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
