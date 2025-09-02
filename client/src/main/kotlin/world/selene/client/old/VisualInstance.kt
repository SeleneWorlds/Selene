package world.selene.client.old

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.GlyphLayout
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Rectangle
import party.iroiro.luajava.Lua
import world.selene.client.data.Anchor
import world.selene.client.data.LabelVisualDefinition
import world.selene.client.rendering.SceneRenderer
import world.selene.client.rendering.VisualContextProvider
import world.selene.common.data.MetadataHolder
import world.selene.common.lua.LuaMappedMetatable
import world.selene.common.lua.LuaMetatable
import world.selene.common.lua.LuaMetatableProvider
import world.selene.common.lua.checkString
import world.selene.common.util.Coordinate

interface VisualInstance {
    val sortLayerOffset: Int
    fun update(delta: Float) {}
    fun render(batch: Batch, x: Float, y: Float, context: VisualContext)
    fun shouldRender(sceneRenderer: SceneRenderer, x: Float, y: Float, context: VisualContext): Boolean
    fun getBounds(context: VisualContext, x: Float, y: Float): Rectangle {
        return getBounds(context, x, y, Rectangle())
    }

    fun getBounds(context: VisualContext, x: Float, y: Float, outRect: Rectangle): Rectangle
    fun occludes(sceneRenderer: SceneRenderer, x: Float, y: Float, context: VisualContext): Boolean
}

interface SizedVisualInstance {
    val width: Float
    val height: Float
}

abstract class TextureBasedVisualInstance(private val metadataHolder: MetadataHolder) : VisualInstance,
    SizedVisualInstance, LuaMetatableProvider {
    abstract val textureRegion: TextureRegion
    abstract val offsetX: Float
    abstract val offsetY: Float
    override val width: Float get() = textureRegion.regionWidth.toFloat()
    override val height: Float get() = textureRegion.regionHeight.toFloat()

    fun getDisplayX(context: VisualContext, x: Float): Float {
        return x + offsetX - textureRegion.regionWidth / 2f
    }

    fun getDisplayY(context: VisualContext, y: Float): Float {
        return y - offsetY - textureRegion.regionHeight - context.offsetY
    }

    override fun getBounds(context: VisualContext, x: Float, y: Float, outRect: Rectangle): Rectangle {
        return outRect.set(getDisplayX(context, x), getDisplayY(context, y), width, height)
    }

    override fun shouldRender(
        sceneRenderer: SceneRenderer,
        x: Float,
        y: Float,
        context: VisualContext
    ): Boolean {
        if (context.interiorFadeAlpha < 0.01f) {
            return false
        }

        return sceneRenderer.cameraManager.isRegionVisible(getBounds(context, x, y))
    }

    override fun occludes(
        sceneRenderer: SceneRenderer,
        x: Float,
        y: Float,
        context: VisualContext
    ): Boolean {
        val thisSortLayer = sceneRenderer.grid.getSortLayer(context.coordinate, 0)
        val focusSortLayer = sceneRenderer.grid.getSortLayer(context.cameraFocusCoordinate, 0)
        if (focusSortLayer - thisSortLayer < sceneRenderer.grid.rowSortScale) {
            return false
        }

        val thisBounds = getBounds(context, x, y)
        val focusBounds = Rectangle()
        val focusedEntity = sceneRenderer.cameraManager.focusedEntity
        val focusedVisual = null // focusedEntity?.mainVisualInstance
        if (focusedVisual != null) {
            // focusedVisual.getBounds(
            //     context.contextProvider.getVisualContext(focusedEntity.coordinate),
            //     focusedEntity.screenX,
            //     focusedEntity.screenY,
            //     focusBounds
            // )
        } else {
            focusBounds.x = sceneRenderer.grid.getScreenX(context.cameraFocusCoordinate)
            focusBounds.y = sceneRenderer.grid.getScreenY(context.cameraFocusCoordinate)
            focusBounds.width = 1f
            focusBounds.height = 1f
        }

        val isLargerThanFocus = thisBounds.height >= focusBounds.height
        val isAboveFocus = (focusedEntity?.coordinate?.z ?: context.coordinate.z) < context.coordinate.z
        if ((isLargerThanFocus || isAboveFocus) && thisBounds.overlaps(focusBounds)) {
            return true
        }
        return false
    }

    override fun render(batch: Batch, x: Float, y: Float, context: VisualContext) {
        batch.color = context.color
        batch.draw(textureRegion, getDisplayX(context, x), getDisplayY(context, y))
    }

    override fun luaMetatable(lua: Lua): LuaMetatable {
        return luaMeta
    }

    companion object {
        val luaMeta = LuaMappedMetatable(TextureBasedVisualInstance::class) {
            callable("GetMetadata") {
                val visual = it.checkSelf()
                val key = it.checkString(2)
                val value = visual.metadataHolder.metadata[key]
                if (value != null) {
                    it.push(value, Lua.Conversion.FULL)
                    return@callable 1
                }
                0
            }
        }
    }
}

data class VisualContext(
    val contextProvider: VisualContextProvider,
    val coordinate: Coordinate,
    val cameraFocusCoordinate: Coordinate,
    var interiorFadeAlpha: Float = 1f,
    var color: Color = Color.WHITE.cpy(),
    var offsetY: Float = 0f,
    var maxWidth: Float = 0f,
    var maxHeight: Float = 0f
)

class LabelVisualInstance(private val visualDef: LabelVisualDefinition, label: String = visualDef.label) :
    VisualInstance, SizedVisualInstance {
    val font = BitmapFont(true)
    val glyphLayout = GlyphLayout(font, label)

    override val sortLayerOffset: Int get() = visualDef.sortLayerOffset
    override val width: Float get() = glyphLayout.width
    override val height: Float get() = glyphLayout.height

    override fun getBounds(
        context: VisualContext,
        x: Float,
        y: Float,
        outRect: Rectangle
    ): Rectangle {
        outRect.set(x, y, width, height)
        return outRect
    }

    override fun shouldRender(
        sceneRenderer: SceneRenderer,
        x: Float,
        y: Float,
        context: VisualContext
    ): Boolean {
        if (context.interiorFadeAlpha < 0.01f) {
            return false
        }

        val regionLeft = x - width / 2f + visualDef.offsetX
        val regionRight = x + width / 2f + visualDef.offsetX
        val regionBottom = y - height - visualDef.offsetY - context.offsetY
        val regionTop = y - visualDef.offsetY - context.offsetY
        return sceneRenderer.cameraManager.isRegionVisible(regionLeft, regionRight, regionBottom, regionTop)
    }

    override fun render(batch: Batch, x: Float, y: Float, context: VisualContext) {
        val effectiveX = x
        var effectiveY = y
        if (visualDef.anchor == Anchor.TOP_CENTER) {
            effectiveY -= context.maxHeight
        }
        font.draw(
            batch,
            glyphLayout,
            effectiveX + visualDef.offsetX - glyphLayout.width / 2f,
            effectiveY + visualDef.offsetY - glyphLayout.height - context.offsetY
        )
    }

    override fun occludes(
        sceneRenderer: SceneRenderer,
        x: Float,
        y: Float,
        context: VisualContext
    ): Boolean {
        return false
    }
}