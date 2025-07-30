package world.selene.client.ui

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import ktx.assets.async.AssetStorage
import world.selene.client.ui.theme.ThemeImpl

class UI(assetStorage: AssetStorage) {
    val rootTheme = ThemeImpl(assetStorage)
    val systemRoot = Node().apply { name = "System"
        width = Gdx.graphics.width.toFloat()
        height = Gdx.graphics.width.toFloat()
    }
    val customRoot = Node().apply { name = "Custom"
        width = Gdx.graphics.width.toFloat()
        height = Gdx.graphics.width.toFloat()
    }
    val root = Node().apply {
        name = "Root"
        theme = rootTheme
        width = Gdx.graphics.width.toFloat()
        height = Gdx.graphics.width.toFloat()
        addChild(customRoot)
        addChild(systemRoot)
    }
    val camera = OrthographicCamera().apply {
        setToOrtho(true)
    }

    fun render(spriteBatch: SpriteBatch) {
        spriteBatch.projectionMatrix = camera.combined
        spriteBatch.color = Color.WHITE
        root.render(spriteBatch)
    }

    fun createNode(nodeDefinition: NodeDefinition): Node {
        val node = when (nodeDefinition.type) {
            "Node" -> Node()
            "Image" -> ImageNode().apply { src = nodeDefinition.attributes["src"] ?: "" }
            else -> throw IllegalArgumentException("Unsupported node type: ${nodeDefinition.type}")
        }
        node.name = nodeDefinition.name
        node.x = nodeDefinition.x
        node.y = nodeDefinition.y
        node.width = nodeDefinition.width
        node.height = nodeDefinition.height
        if (node.width == 0f || node.height == 0f) {
            node.fitToContent()
        }
        node.anchor = nodeDefinition.anchor
        node.theme = rootTheme
        for (child in nodeDefinition.children) {
            node.addChild(createNode(child))
        }
        return node
    }
}