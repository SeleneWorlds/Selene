package world.selene.client.ui

import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.math.Vector2
import world.selene.client.ui.theme.NoopTheme
import world.selene.client.ui.theme.Theme

open class Node {
    var name = "Node"
    var theme: Theme = NoopTheme
        set(value) {
            field = value
            themeChanged()
        }
    var parent: Node? = null; private set
    val children = mutableListOf<Node>()
    val luaProxy = NodeLuaProxy(this)

    enum class Anchor {
        TOP_LEFT, TOP, TOP_RIGHT,
        LEFT, CENTER, RIGHT,
        BOTTOM_LEFT, BOTTOM, BOTTOM_RIGHT
    }

    var anchor: Anchor = Anchor.TOP_LEFT
        set(value) {
            field = value
            markAbsolutePositionDirty()
        }
    var x: Float = 0f
        set(value) {
            field = value
            markAbsolutePositionDirty()
        }
    var y: Float = 0f
        set(value) {
            field = value
            markAbsolutePositionDirty()
        }
    var width: Float = 0f
        set(value) {
            field = value
            markAbsolutePositionDirty()
        }
    var height: Float = 0f
        set(value) {
            field = value
            markAbsolutePositionDirty()
        }

    private var _absolutePosition: Vector2 = Vector2.Zero.cpy()
    private var absolutePositionDirty: Boolean = true
    val absolutePosition: Vector2
        get() {
            if (absolutePositionDirty) {
                updateAbsolutePosition()
            }
            return _absolutePosition
        }

    private fun updateAbsolutePosition() {
        val parentAbs = parent?.absolutePosition ?: Vector2.Zero
        val anchorOffset = parent?.getAnchorOffset(anchor) ?: Vector2.Zero
        _absolutePosition.set(parentAbs.x + x + anchorOffset.x, parentAbs.y + y + anchorOffset.y)
        absolutePositionDirty = false
    }

    fun markAbsolutePositionDirty() {
        absolutePositionDirty = true
        children.forEach { it.markAbsolutePositionDirty() }
    }

    open fun themeChanged() {
    }

    fun addChild(node: Node) {
        node.parent = this
        children.add(node)
    }

    fun removeChild(node: Node) {
        node.parent = null
        children.remove(node)
    }

    /**
     * Returns the offset for the current anchor.
     */
    open fun getAnchorOffset(anchor: Anchor): Vector2 {
        // Calculates offset based on anchor and node size
        return when (anchor) {
            Anchor.TOP_LEFT -> Vector2(0f, 0f)
            Anchor.TOP -> Vector2(width / 2f, 0f)
            Anchor.TOP_RIGHT -> Vector2(width, 0f)
            Anchor.LEFT -> Vector2(0f, height / 2f)
            Anchor.CENTER -> Vector2(width / 2f, height / 2f)
            Anchor.RIGHT -> Vector2(width, height / 2f)
            Anchor.BOTTOM_LEFT -> Vector2(0f, height)
            Anchor.BOTTOM -> Vector2(width / 2f, height)
            Anchor.BOTTOM_RIGHT -> Vector2(width, height)
        }
    }

    fun render(spriteBatch: SpriteBatch) {
        renderBackground(spriteBatch)
        children.forEach { it.render(spriteBatch) }
        renderForeground(spriteBatch)
    }

    open fun renderBackground(spriteBatch: SpriteBatch) {}
    open fun renderForeground(spriteBatch: SpriteBatch) {}

    override fun toString(): String {
        return "Node(name=$name, children=$children, absPos=$absolutePosition)"
    }

    open fun fitToContent() {
    }

    class NodeLuaProxy(private val delegate: Node) {
        fun AddChild(node: NodeLuaProxy) {
            delegate.addChild(node.delegate)
        }

        override fun toString(): String {
            return delegate.name
        }
    }
}