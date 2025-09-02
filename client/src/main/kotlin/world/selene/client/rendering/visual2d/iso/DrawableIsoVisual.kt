package world.selene.client.rendering.visual2d.iso

import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.math.Rectangle
import party.iroiro.luajava.Lua
import world.selene.client.data.VisualDefinition
import world.selene.client.rendering.drawable.Drawable
import world.selene.common.lua.LuaMetatable

class DrawableIsoVisual(
    private val visualDefinition: VisualDefinition,
    private val drawable: Drawable,
    override val sortLayerOffset: Int,
    override val surfaceHeight: Float
) : IsoVisual {
    var shouldUpdate = true
    override fun update(delta: Float) {
        if (shouldUpdate) {
            drawable.update(delta)
        }
    }

    override fun getBounds(
        x: Float,
        y: Float,
        outRect: Rectangle
    ): Rectangle {
        return drawable.getBounds(x, y, outRect)
    }

    override fun render(batch: Batch, x: Float, y: Float) {
        drawable.render(batch, x, y)
    }

    override fun luaMetatable(lua: Lua): LuaMetatable {
        return luaMeta
    }

    companion object {
        val luaMeta = IsoVisual.luaMeta.extend(DrawableIsoVisual::class) {
            readOnly(DrawableIsoVisual::drawable)
            readOnly(DrawableIsoVisual::visualDefinition, "Definition")
        }
    }
}

