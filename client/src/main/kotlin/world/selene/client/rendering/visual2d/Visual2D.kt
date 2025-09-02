package world.selene.client.rendering.visual2d

import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.math.Rectangle
import party.iroiro.luajava.Lua
import world.selene.client.rendering.visual.Visual
import world.selene.common.lua.LuaMappedMetatable
import world.selene.common.lua.LuaMetatable
import world.selene.common.lua.LuaMetatableProvider

interface Visual2D : Visual, LuaMetatableProvider {
    fun render(batch: Batch, x: Float, y: Float)
    fun getBounds(x: Float, y: Float, outRect: Rectangle): Rectangle

    override fun luaMetatable(lua: Lua): LuaMetatable {
        return luaMeta
    }

    companion object {
        val luaMeta = LuaMappedMetatable(Visual2D::class) {
        }
    }
}