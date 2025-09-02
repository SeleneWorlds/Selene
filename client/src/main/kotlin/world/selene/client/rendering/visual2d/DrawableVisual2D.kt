package world.selene.client.rendering.visual2d

import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.math.Rectangle
import party.iroiro.luajava.Lua
import world.selene.client.data.VisualDefinition
import world.selene.client.rendering.drawable.Drawable
import world.selene.common.lua.LuaMappedMetatable
import world.selene.common.lua.LuaMetatable
import world.selene.common.lua.LuaMetatableProvider

class DrawableVisual2D(
    private val visualDefinition: VisualDefinition,
    private val drawable: Drawable
) : Visual2D, LuaMetatableProvider {
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
        val luaMeta = LuaMappedMetatable(DrawableVisual2D::class) {
            readOnly(DrawableVisual2D::drawable)
            readOnly(DrawableVisual2D::visualDefinition, "Definition")
        }
    }
}

