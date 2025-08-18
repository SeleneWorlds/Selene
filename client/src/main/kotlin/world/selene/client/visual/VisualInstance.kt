package world.selene.client.visual

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.GlyphLayout
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Rectangle
import party.iroiro.luajava.Lua
import world.selene.client.animator.Animator
import world.selene.client.assets.AssetProvider
import world.selene.client.data.Anchor
import world.selene.client.data.AnimatedVisualDefinition
import world.selene.client.data.AnimatorVisualDefinition
import world.selene.client.data.LabelVisualDefinition
import world.selene.client.data.SimpleVisualDefinition
import world.selene.client.data.VariantsVisualDefinition
import world.selene.client.rendering.SceneRenderer
import world.selene.client.rendering.VisualContextProvider
import world.selene.common.lua.LuaManager
import world.selene.common.lua.LuaMappedMetatable
import world.selene.common.lua.LuaMetatable
import world.selene.common.lua.LuaMetatableProvider
import world.selene.common.lua.Signal
import world.selene.common.util.Coordinate

interface VisualInstance {
    val sortLayerOffset: Int
    fun updateShared(delta: Float) {}
    fun update(delta: Float) {}
    fun render(spriteBatch: SpriteBatch, x: Float, y: Float, context: VisualContext)
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

abstract class TextureBasedVisualInstance : VisualInstance, SizedVisualInstance {
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
        val focusedVisual = focusedEntity?.mainVisualInstance
        if (focusedVisual != null) {
            focusedVisual.getBounds(
                context.contextProvider.getVisualContext(focusedEntity.coordinate),
                focusedEntity.screenX,
                focusedEntity.screenY,
                focusBounds
            )
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

    override fun render(spriteBatch: SpriteBatch, x: Float, y: Float, context: VisualContext) {
        spriteBatch.color = context.color
        spriteBatch.draw(textureRegion, getDisplayX(context, x), getDisplayY(context, y))
    }
}

data class SimpleVisualInstance(
    private val visualDef: SimpleVisualDefinition,
    private val assetProvider: AssetProvider
) : TextureBasedVisualInstance(), SizedVisualInstance {
    override val textureRegion: TextureRegion by lazy {
        val texturePath = visualDef.texture
        assetProvider.loadTextureRegion(texturePath) ?: assetProvider.missingTexture
    }

    override val offsetX: Float get() = visualDef.offsetX.toFloat()
    override val offsetY: Float get() = visualDef.offsetY.toFloat()
    override val sortLayerOffset: Int get() = visualDef.sortLayerOffset

    override fun render(spriteBatch: SpriteBatch, x: Float, y: Float, context: VisualContext) {
        super.render(spriteBatch, x, y, context)
        context.offsetY += visualDef.surfaceOffsetY
    }
}

data class VariantsVisualInstance(
    private val visualDef: VariantsVisualDefinition,
    private val assetProvider: AssetProvider
) : TextureBasedVisualInstance(), SizedVisualInstance {
    override val textureRegion: TextureRegion by lazy {
        val texturePath = visualDef.textures.first()
        assetProvider.loadTextureRegion(texturePath) ?: assetProvider.missingTexture
    }
    override val offsetX: Float get() = visualDef.offsetX.toFloat()
    override val offsetY: Float get() = visualDef.offsetY.toFloat()

    override val sortLayerOffset: Int get() = visualDef.sortLayerOffset

    override fun render(spriteBatch: SpriteBatch, x: Float, y: Float, context: VisualContext) {
        super.render(spriteBatch, x, y, context)
        context.offsetY += visualDef.surfaceOffsetY
    }
}

data class AnimatedVisualInstance(
    private val visualDef: AnimatedVisualDefinition,
    private val assetProvider: AssetProvider,
    private val luaManager: LuaManager
) : TextureBasedVisualInstance(), SizedVisualInstance, LuaMetatableProvider {
    val animationCompleted = Signal("AnimationCompleted")

    private val textureRegions: List<TextureRegion> by lazy {
        visualDef.textures.map { path ->
            assetProvider.loadTextureRegion(path) ?: assetProvider.missingTexture
        }
    }
    private var currentFrame = 0
    private var elapsedTime = 0f

    override val sortLayerOffset: Int get() = visualDef.sortLayerOffset
    override val textureRegion: TextureRegion get() = textureRegions[currentFrame]
    override val offsetX: Float get() = visualDef.offsetX.toFloat()
    override val offsetY: Float get() = visualDef.offsetY.toFloat()

    override fun updateShared(delta: Float) {
        if (textureRegions.size <= 1) return
        elapsedTime += delta
        val frameDuration = visualDef.duration
        while (elapsedTime >= frameDuration) {
            elapsedTime -= frameDuration
            currentFrame = (currentFrame + 1) % textureRegions.size
            if (currentFrame == 0) {
                animationCompleted.emit()
            }
        }
    }

    override fun render(spriteBatch: SpriteBatch, x: Float, y: Float, context: VisualContext) {
        super.render(spriteBatch, x, y, context)
        context.offsetY += visualDef.surfaceOffsetY
    }

    override fun luaMetatable(lua: Lua): LuaMetatable {
        return luaMeta
    }

    companion object {
        val luaMeta = LuaMappedMetatable(AnimatedVisualInstance::class) {
            readOnly(AnimatedVisualInstance::currentFrame)
            readOnly(AnimatedVisualInstance::elapsedTime)
            readOnly(AnimatedVisualInstance::animationCompleted)
        }
    }
}

class AnimatorVisualInstance(
    private val visualDef: AnimatorVisualDefinition,
    private val assetProvider: AssetProvider
) : TextureBasedVisualInstance(), SizedVisualInstance {
    lateinit var animator: Animator
    private var currentAnimation = ""
    private var frameIndex = 0
    private var time = 0f
    private val regions: Map<String, List<TextureRegion>> = visualDef.animations.mapValues { (_, frames) ->
        frames.textures.map { path ->
            assetProvider.loadTextureRegion(path) ?: assetProvider.missingTexture
        }
    }
    private val speeds: Map<String, Float> = visualDef.animations.mapValues { it.value.speed ?: 0.13f }

    override val textureRegion: TextureRegion
        get() = regions[currentAnimation]?.getOrNull(frameIndex) ?: regions.values.first().first()

    override val sortLayerOffset: Int get() = visualDef.sortLayerOffset
    val animFrames get() = visualDef.animations[currentAnimation]
    override val offsetX: Float get() = (animFrames?.offsetX ?: visualDef.offsetX).toFloat()
    override val offsetY: Float get() = (animFrames?.offsetY ?: visualDef.offsetY).toFloat()

    override fun render(spriteBatch: SpriteBatch, x: Float, y: Float, context: VisualContext) {
        super.render(spriteBatch, x, y, context)
        context.offsetY += visualDef.surfaceOffsetY
    }

    override fun update(delta: Float) {
        val nextAnimation = animator.getAnimation()
        if (currentAnimation != nextAnimation) {
            currentAnimation = nextAnimation
            frameIndex = 0
            time = 0f
        }

        val frames = regions[currentAnimation] ?: return
        val speed = speeds[currentAnimation] ?: 0.13f
        time += delta
        if (time >= speed) {
            time = 0f
            frameIndex = (frameIndex + 1) % frames.size
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

    override fun render(spriteBatch: SpriteBatch, x: Float, y: Float, context: VisualContext) {
        val effectiveX = x
        var effectiveY = y
        if (visualDef.anchor == Anchor.TOP_CENTER) {
            effectiveY -= context.maxHeight
        }
        font.draw(
            spriteBatch,
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