package world.selene.client.rendering.visual2d.iso

import com.badlogic.gdx.graphics.g2d.Batch
import party.iroiro.luajava.Lua
import world.selene.client.rendering.drawable.Drawable
import world.selene.common.lua.LuaMetatable

class DynamicDrawableIsoVisual(
    private val drawableProvider: () -> Drawable?,
    override val sortLayerOffset: Int,
    override val surfaceHeight: Float
) : IsoVisual {

    private val drawable: Drawable? get() = drawableProvider()

    var shouldUpdate = true
    override fun update(delta: Float) {
        if (shouldUpdate) {
            drawable?.update(delta)
        }
    }

    override fun render(batch: Batch, x: Float, y: Float) {
        drawable?.render(batch, x, y)
    }

    override fun luaMetatable(lua: Lua): LuaMetatable {
        return luaMeta
    }

    companion object {
        val luaMeta = IsoVisual.luaMeta.extend(DynamicDrawableIsoVisual::class) {
            readOnly(DynamicDrawableIsoVisual::drawable)
        }
    }
}

