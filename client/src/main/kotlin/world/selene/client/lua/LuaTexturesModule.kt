package world.selene.client.lua

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Colors
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.utils.Disposable
import party.iroiro.luajava.Lua
import party.iroiro.luajava.value.LuaValue
import world.selene.common.lua.LuaManager
import world.selene.common.lua.LuaModule
import world.selene.common.lua.checkInt
import world.selene.common.lua.checkString
import world.selene.common.lua.getFieldFloat
import world.selene.common.lua.register

class LuaTexturesModule : LuaModule {
    override val name = "selene.textures"

    override fun initialize(luaManager: LuaManager) {
        luaManager.exposeClass(TextureLuaProxy::class)
    }

    override fun register(table: LuaValue) {
        table.register("Create", this::luaCreateTexture)
    }

    private fun luaCreateTexture(lua: Lua): Int {
        val width = lua.checkInt(1)
        val height = lua.checkInt(2)
        val formatName = if (lua.top >= 3) lua.checkString(3) else "RGBA8888"
        val format = when (formatName) {
            "RGBA8888" -> Pixmap.Format.RGBA8888
            "RGB888" -> Pixmap.Format.RGB888
            "RGBA4444" -> Pixmap.Format.RGBA4444
            "RGB565" -> Pixmap.Format.RGB565
            "Alpha" -> Pixmap.Format.Alpha
            else -> Pixmap.Format.RGBA8888
        }

        try {
            val pixmap = Pixmap(width, height, format)
            val texture = Texture(pixmap)
            val proxy = TextureLuaProxy(texture, pixmap)
            lua.push(proxy, Lua.Conversion.NONE)
            return 1
        } catch (e: Exception) {
            return lua.error(RuntimeException("Failed to create texture: ${e.message}", e))
        }
    }

    class TextureLuaProxy(val texture: Texture, private val pixmap: Pixmap) : Disposable {
        private var disposed = false

        val Width: Int
            get() = texture.width

        val Height: Int
            get() = texture.height

        fun SetPixel(lua: Lua): Int {
            if (disposed) {
                return lua.error(IllegalStateException("Texture has been disposed"))
            }

            val x = lua.checkInt(2)
            val y = lua.checkInt(3)
            val color = parseColor(lua, 4)

            try {
                pixmap.setColor(color)
                pixmap.drawPixel(x, y)
                return 0
            } catch (e: Exception) {
                return lua.error(RuntimeException("Failed to set pixel: ${e.message}", e))
            }
        }

        fun GetPixel(lua: Lua): Int {
            if (disposed) {
                return lua.error(IllegalStateException("Texture has been disposed"))
            }

            val x = lua.checkInt(2)
            val y = lua.checkInt(3)

            try {
                val colorInt = pixmap.getPixel(x, y)
                val color = Color(colorInt)
                lua.push(color.r)
                lua.push(color.g)
                lua.push(color.b)
                lua.push(color.a)
                return 4
            } catch (e: Exception) {
                return lua.error(RuntimeException("Failed to get pixel: ${e.message}", e))
            }
        }

        fun Fill(lua: Lua): Int {
            if (disposed) {
                return lua.error(IllegalStateException("Texture has been disposed"))
            }

            val color = parseColor(lua, 2)

            try {
                pixmap.setColor(color)
                pixmap.fill()
                return 0
            } catch (e: Exception) {
                return lua.error(RuntimeException("Failed to fill texture: ${e.message}", e))
            }
        }

        fun DrawRect(lua: Lua): Int {
            if (disposed) {
                return lua.error(IllegalStateException("Texture has been disposed"))
            }

            val x = lua.checkInt(2)
            val y = lua.checkInt(3)
            val width = lua.checkInt(4)
            val height = lua.checkInt(5)
            val color = parseColor(lua, 6)

            try {
                pixmap.setColor(color)
                pixmap.drawRectangle(x, y, width, height)
                return 0
            } catch (e: Exception) {
                return lua.error(RuntimeException("Failed to draw rectangle: ${e.message}", e))
            }
        }

        fun FillRect(lua: Lua): Int {
            if (disposed) {
                return lua.error(IllegalStateException("Texture has been disposed"))
            }

            val x = lua.checkInt(2)
            val y = lua.checkInt(3)
            val width = lua.checkInt(4)
            val height = lua.checkInt(5)
            val color = parseColor(lua, 6)

            try {
                pixmap.setColor(color)
                pixmap.fillRectangle(x, y, width, height)
                return 0
            } catch (e: Exception) {
                return lua.error(RuntimeException("Failed to fill rectangle: ${e.message}", e))
            }
        }

        fun DrawCircle(lua: Lua): Int {
            if (disposed) {
                return lua.error(IllegalStateException("Texture has been disposed"))
            }

            val x = lua.checkInt(2)
            val y = lua.checkInt(3)
            val radius = lua.checkInt(4)
            val color = parseColor(lua, 5)

            try {
                pixmap.setColor(color)
                pixmap.drawCircle(x, y, radius)
                return 0
            } catch (e: Exception) {
                return lua.error(RuntimeException("Failed to draw circle: ${e.message}", e))
            }
        }

        fun FillCircle(lua: Lua): Int {
            if (disposed) {
                return lua.error(IllegalStateException("Texture has been disposed"))
            }

            val x = lua.checkInt(2)
            val y = lua.checkInt(3)
            val radius = lua.checkInt(4)
            val color = parseColor(lua, 5)

            try {
                pixmap.setColor(color)
                pixmap.fillCircle(x, y, radius)
                return 0
            } catch (e: Exception) {
                return lua.error(RuntimeException("Failed to fill circle: ${e.message}", e))
            }
        }

        fun DrawLine(lua: Lua): Int {
            if (disposed) {
                return lua.error(IllegalStateException("Texture has been disposed"))
            }

            val x1 = lua.checkInt(2)
            val y1 = lua.checkInt(3)
            val x2 = lua.checkInt(4)
            val y2 = lua.checkInt(5)
            val color = parseColor(lua, 6)

            try {
                pixmap.setColor(color)
                pixmap.drawLine(x1, y1, x2, y2)
                return 0
            } catch (e: Exception) {
                return lua.error(RuntimeException("Failed to draw line: ${e.message}", e))
            }
        }

        fun Update() {
            texture.draw(pixmap, 0, 0)
        }

        fun Dispose() {
            dispose()
        }

        override fun dispose() {
            if (!disposed) {
                texture.dispose()
                pixmap.dispose()
                disposed = true
            }
        }

        private fun parseColor(lua: Lua, index: Int): Color {
            return when {
                lua.isString(index) -> {
                    val colorString = lua.toString(index)!!
                    when {
                        colorString.startsWith("#") -> {
                            try {
                                val hex = colorString.substring(1)
                                when (hex.length) {
                                    6 -> Color.valueOf(hex + "FF")
                                    8 -> Color.valueOf(hex)
                                    else -> throw IllegalArgumentException("Invalid hex color: $colorString")
                                }
                            } catch (e: Exception) {
                                throw IllegalArgumentException("Invalid hex color: $colorString", e)
                            }
                        }

                        else -> {
                            return Colors.get(colorString.uppercase())
                                ?: throw IllegalArgumentException("Unknown color name: $colorString")
                        }
                    }
                }

                lua.isTable(index) -> {
                    val r = lua.getFieldFloat(index, "r") ?: lua.getFieldFloat(index, "red") ?: 1f
                    val g = lua.getFieldFloat(index, "g") ?: lua.getFieldFloat(index, "green") ?: 1f
                    val b = lua.getFieldFloat(index, "b") ?: lua.getFieldFloat(index, "blue") ?: 1f
                    val a = lua.getFieldFloat(index, "a") ?: lua.getFieldFloat(index, "alpha") ?: 1f
                    Color(r, g, b, a)
                }

                else -> {
                    throw IllegalArgumentException("Invalid color type: ${lua.type(index)}")
                }
            }
        }
    }
}
