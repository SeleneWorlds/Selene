package world.selene.client.rendering.drawable

import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.math.Rectangle
import party.iroiro.luajava.Lua
import world.selene.common.lua.LuaMappedMetatable
import world.selene.common.lua.LuaMetatable
import world.selene.common.lua.LuaMetatableProvider

interface Drawable : LuaMetatableProvider {
    fun getBounds(x: Float, y: Float, outRect: Rectangle): Rectangle
    fun update(delta: Float)
    fun render(batch: Batch, x: Float, y: Float)
    fun render(batch: Batch, x: Float, y: Float, width: Float, height: Float)
    fun render(
        batch: Batch, x: Float, y: Float,
        originX: Float, originY: Float,
        width: Float, height: Float,
        scaleX: Float, scaleY: Float,
        rotation: Float
    )

    override fun luaMetatable(lua: Lua): LuaMetatable {
        return luaMeta
    }

    companion object {
        val luaMeta = LuaMappedMetatable(Drawable::class) {
        }
    }

    object Empty : Drawable {
        override fun getBounds(
            x: Float,
            y: Float,
            outRect: Rectangle
        ): Rectangle {
            return outRect.set(x, y, 0f, 0f)
        }

        override fun update(delta: Float) = Unit

        override fun render(batch: Batch, x: Float, y: Float) = Unit

        override fun render(
            batch: Batch,
            x: Float,
            y: Float,
            width: Float,
            height: Float
        ) = Unit

        override fun render(
            batch: Batch,
            x: Float,
            y: Float,
            originX: Float,
            originY: Float,
            width: Float,
            height: Float,
            scaleX: Float,
            scaleY: Float,
            rotation: Float
        ) = Unit

    }
}