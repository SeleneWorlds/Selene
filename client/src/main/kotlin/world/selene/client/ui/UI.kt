package world.selene.client.ui

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.*
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.Group
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.badlogic.gdx.utils.viewport.ScreenViewport
import org.slf4j.Logger
import world.selene.client.assets.AssetProvider

class UI(private val assetProvider: AssetProvider, private val logger: Logger) {

    val stage: Stage = Stage(ScreenViewport())

    val skin: Skin = createSkin()

    val bundlesRoot = Stack().apply {
        name = "Bundles"
    }

    val systemRoot = Stack().apply {
        name = "System"
    }

    val root = Stack().apply {
        name = "Root"
        add(bundlesRoot)
        add(systemRoot)
    }

    init {
        stage.addActor(root)
        // TODO stage is an InputProcessor - need to use a multiplexer
    }

    private fun createSkin(): Skin {
        // TODO apparently skins can be loaded from json, should do that instead
        val skin = Skin()

        skin.add("white", Color.WHITE)
        skin.add("gray", Color.GRAY)
        skin.add("dark-gray", Color.DARK_GRAY)
        skin.add("light-gray", Color.LIGHT_GRAY)
        skin.add("black", Color.BLACK)

        val pixelTexture = Pixmap(1, 1, Pixmap.Format.RGBA8888).apply {
            setColor(Color.WHITE)
            fill()
        }
        val texture = Texture(pixelTexture)
        pixelTexture.dispose()

        val whitePixel = TextureRegionDrawable(texture)
        skin.add("white-pixel", whitePixel)

        val defaultFont = BitmapFont()
        skin.add("default-font", defaultFont)

        val labelStyle = Label.LabelStyle().apply {
            font = defaultFont
            fontColor = Color.BLACK
        }
        skin.add("default", labelStyle)

        val buttonStyle = TextButton.TextButtonStyle().apply {
            font = defaultFont
            fontColor = Color.BLACK
            up = whitePixel
            down = whitePixel
            over = whitePixel
        }
        skin.add("default", buttonStyle)

        val imageButtonStyle = ImageButton.ImageButtonStyle().apply {
            up = whitePixel
            down = whitePixel
            over = whitePixel
        }
        skin.add("default", imageButtonStyle)

        val windowStyle = Window.WindowStyle().apply {
            titleFont = defaultFont
            titleFontColor = Color.BLACK
            background = whitePixel
        }
        skin.add("default", windowStyle)

        return skin
    }

    fun render() {
        stage.act(Gdx.graphics.deltaTime)
        stage.draw()
    }

    fun resize(width: Int, height: Int) {
        //stage.viewport.update(width, height, true)
    }

    fun dispose() {
        stage.dispose()
        skin.dispose()
    }

    fun createActor(nodeDefinition: NodeDefinition): Actor {
        val actor = when (nodeDefinition.type) {
            "Container" -> createContainer(nodeDefinition)
            "Group" -> createGroup(nodeDefinition)
            "Image" -> createImage(nodeDefinition)
            else -> throw IllegalArgumentException("Unsupported node type: ${nodeDefinition.type}")
        }

        if (nodeDefinition.children.isNotEmpty()) {
            for (childDefinition in nodeDefinition.children) {
                val childActor = createActor(childDefinition)
                if (actor is Group) {
                    actor.addActor(childActor)
                } else {
                    logger.warn("Element of type ${actor.javaClass.simpleName} cannot have children")
                }
            }
        }

        return actor
    }

    private fun createGroup(nodeDefinition: NodeDefinition): Group {
        return Group().apply {
            name = nodeDefinition.name
            setPosition(nodeDefinition.x, nodeDefinition.y)
            setSize(nodeDefinition.width, nodeDefinition.height)
        }
    }

    private fun createContainer(nodeDefinition: NodeDefinition): Container<Actor> {
        return Container<Actor>().apply {
            name = nodeDefinition.name
            setPosition(nodeDefinition.x, nodeDefinition.y)
            setSize(nodeDefinition.width, nodeDefinition.height)
        }
    }

    private fun createImage(nodeDefinition: NodeDefinition): Image {
        val src = nodeDefinition.attributes["src"] ?: ""
        val textureRegion = if (src.isNotEmpty()) {
            assetProvider.loadTextureRegion(src)?.apply { flip(false, true) } ?: assetProvider.missingTexture
        } else {
            assetProvider.missingTexture
        }
        val drawable = TextureRegionDrawable(textureRegion)

        return Image(drawable).apply {
            name = nodeDefinition.name
            setPosition(nodeDefinition.x, nodeDefinition.y)
            if (nodeDefinition.width > 0) width = nodeDefinition.width
            if (nodeDefinition.height > 0) height = nodeDefinition.height
        }
    }
}
