package world.selene.client.lua

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import party.iroiro.luajava.Lua
import world.selene.common.lua.LuaMappedMetatable
import world.selene.common.lua.LuaMetatable
import world.selene.common.lua.LuaMetatableProvider
import world.selene.common.lua.checkInt
import world.selene.common.lua.checkUserdata

data class LuaTexture(val texture: Texture, val pixmap: Pixmap) : LuaMetatableProvider {
    val width: Int get() = texture.width
    val height: Int get() = texture.height

    override fun luaMetatable(lua: Lua): LuaMetatable {
        return luaMeta
    }

    companion object {
        private fun luaSetPixel(lua: Lua): Int {
            val self = lua.checkUserdata<LuaTexture>(1)
            val x = lua.checkInt(2)
            val y = lua.checkInt(3)
            val (color, _) = lua.checkColor(4)
            self.pixmap.setColor(color)
            self.pixmap.drawPixel(x, y)
            return 0
        }

        private fun luaGetPixel(lua: Lua): Int {
            val self = lua.checkUserdata<LuaTexture>(1)
            val x = lua.checkInt(2)
            val y = lua.checkInt(3)
            val colorInt = self.pixmap.getPixel(x, y)
            val color = Color(colorInt)
            lua.push(color.r)
            lua.push(color.g)
            lua.push(color.b)
            lua.push(color.a)
            return 4
        }

        private fun luaFill(lua: Lua): Int {
            val self = lua.checkUserdata<LuaTexture>(1)
            val (color, _) = lua.checkColor(2)
            self.pixmap.setColor(color)
            self.pixmap.fill()
            return 0
        }

        private fun luaCopyFrom(lua: Lua): Int {
            val self = lua.checkUserdata<LuaTexture>(1)
            val sourceTexture = lua.checkUserdata<LuaTexture>(2)
            val srcX = lua.checkInt(3)
            val srcY = lua.checkInt(4)
            val srcWidth = lua.checkInt(5)
            val srcHeight = lua.checkInt(6)
            val dstX = lua.checkInt(7)
            val dstY = lua.checkInt(8)
            self.pixmap.drawPixmap(
                sourceTexture.pixmap,
                srcX,
                srcY,
                srcWidth,
                srcHeight,
                dstX,
                dstY,
                srcWidth,
                srcHeight
            )
            return 0
        }

        private fun luaUpdate(lua: Lua): Int {
            val self = lua.checkUserdata<LuaTexture>(1)
            self.texture.draw(self.pixmap, 0, 0)
            return 0
        }

        private fun luaDispose(lua: Lua): Int {
            val self = lua.checkUserdata<LuaTexture>(1)
            self.texture.dispose()
            self.pixmap.dispose()
            return 0
        }

        val luaMeta = LuaMappedMetatable(LuaTexture::class) {
            readOnly(LuaTexture::width)
            readOnly(LuaTexture::height)
            callable(::luaSetPixel)
            callable(::luaGetPixel)
            callable(::luaFill)
            callable(::luaCopyFrom)
            callable(::luaUpdate)
            callable(::luaDispose)
        }
    }
}