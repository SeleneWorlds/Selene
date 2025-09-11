package world.selene.client.rendering.lua

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import party.iroiro.luajava.Lua
import world.selene.client.lua.checkColor
import world.selene.common.lua.*
import world.selene.common.lua.util.checkInt
import world.selene.common.lua.util.checkUserdata

@Suppress("SameReturnValue")
data class LuaTexture(val texture: Texture, val pixmap: Pixmap) : LuaMetatableProvider {
    val width: Int get() = texture.width
    val height: Int get() = texture.height

    override fun luaMetatable(lua: Lua): LuaMetatable {
        return luaMeta
    }

    companion object {
        /**
         * Width of the texture.
         *
         * ```property
         * Width: number
         * ```
         */
        private fun luaGetWidth(lua: Lua): Int {
            val self = lua.checkUserdata<LuaTexture>(1)
            lua.push(self.width)
            return 1
        }

        /**
         * Height of the texture.
         *
         * ```property
         * Height: number
         * ```
         */
        private fun luaGetHeight(lua: Lua): Int {
            val self = lua.checkUserdata<LuaTexture>(1)
            lua.push(self.height)
            return 1
        }

        /**
         * Sets the color of a pixel at the specified coordinates.
         *
         * ```signatures
         * SetPixel(x: number, y: number, color: Color)
         * SetPixel(x: number, y: number, color: table{r: number|nil, g: number|nil, b: number|nil, a: number|nil})
         * SetPixel(x: number, y: number, color: table{red: number|nil, green: number|nil, blue: number|nil, alpha: number|nil})
         * SetPixel(x: number, y: number, hexColor: string)
         * ```
         */
        private fun luaSetPixel(lua: Lua): Int {
            val self = lua.checkUserdata<LuaTexture>(1)
            val x = lua.checkInt(2)
            val y = lua.checkInt(3)
            val (color, _) = lua.checkColor(4)
            self.pixmap.setColor(color)
            self.pixmap.drawPixel(x, y)
            return 0
        }

        /**
         * Returns the color of a pixel at the specified coordinates.
         * Returns RGBA values as separate numbers.
         *
         * ```signatures
         * GetPixel(x: number, y: number) -> red: number, green: number, blue: number, alpha: number
         * ```
         */
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

        /**
         * Fills the entire texture with the specified color.
         *
         * ```signatures
         * Fill(color: Color)
         * Fill(color: table{r: number|nil, g: number|nil, b: number|nil, a: number|nil})
         * Fill(color: table{red: number|nil, green: number|nil, blue: number|nil, alpha: number|nil})
         * Fill(hexColor: string)
         * ```
         */
        private fun luaFill(lua: Lua): Int {
            val self = lua.checkUserdata<LuaTexture>(1)
            val (color, _) = lua.checkColor(2)
            self.pixmap.setColor(color)
            self.pixmap.fill()
            return 0
        }

        /**
         * Copies pixels from another texture to this texture.
         *
         * ```signatures
         * CopyFrom(sourceTexture: LuaTexture, srcX: number, srcY: number, srcWidth: number, srcHeight: number, dstX: number, dstY: number)
         * ```
         */
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

        /**
         * Updates the GPU texture with the current pixmap data.
         * Call this after making pixel modifications to see changes.
         *
         * ```signatures
         * Update()
         * ```
         */
        private fun luaUpdate(lua: Lua): Int {
            val self = lua.checkUserdata<LuaTexture>(1)
            self.texture.draw(self.pixmap, 0, 0)
            return 0
        }

        /**
         * Disposes the texture and pixmap, freeing GPU and system memory.
         * The texture becomes unusable after calling this.
         *
         * ```signatures
         * Dispose()
         * ```
         */
        private fun luaDispose(lua: Lua): Int {
            val self = lua.checkUserdata<LuaTexture>(1)
            self.texture.dispose()
            self.pixmap.dispose()
            return 0
        }

        val luaMeta = LuaMappedMetatable(LuaTexture::class) {
            getter(::luaGetWidth)
            getter(::luaGetHeight)
            callable(::luaSetPixel)
            callable(::luaGetPixel)
            callable(::luaFill)
            callable(::luaCopyFrom)
            callable(::luaUpdate)
            callable(::luaDispose)
        }
    }
}