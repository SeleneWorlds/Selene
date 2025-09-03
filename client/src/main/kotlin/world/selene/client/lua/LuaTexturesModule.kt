package world.selene.client.lua

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import party.iroiro.luajava.Lua
import party.iroiro.luajava.value.LuaValue
import world.selene.common.lua.LuaMappedMetatable
import world.selene.common.lua.LuaMetatable
import world.selene.common.lua.LuaMetatableProvider
import world.selene.common.lua.LuaModule
import world.selene.common.lua.checkInt
import world.selene.common.lua.checkUserdata
import world.selene.common.lua.checkString
import world.selene.common.lua.register

/**
 * Provides texture creation and manipulation functionality.
 */
class LuaTexturesModule : LuaModule {
    override val name = "selene.textures"

    data class LuaTexture(val texture: Texture, val pixmap: Pixmap) : LuaMetatableProvider {
        val width: Int get() = texture.width
        val height: Int get() = texture.height

        override fun luaMetatable(lua: Lua): LuaMetatable {
            return luaMeta
        }

        companion object {
            val luaMeta = LuaMappedMetatable(LuaTexture::class) {
                readOnly(LuaTexture::width)
                readOnly(LuaTexture::height)
                callable("SetPixel") {
                    val self = it.checkSelf()
                    val x = it.checkInt(2)
                    val y = it.checkInt(3)
                    val (color, _) = it.checkColor(4)
                    self.pixmap.setColor(color)
                    self.pixmap.drawPixel(x, y)
                    0
                }
                callable("GetPixel") {
                    val self = it.checkSelf()
                    val x = it.checkInt(2)
                    val y = it.checkInt(3)
                    val colorInt = self.pixmap.getPixel(x, y)
                    val color = Color(colorInt)
                    it.push(color.r)
                    it.push(color.g)
                    it.push(color.b)
                    it.push(color.a)
                    4
                }
                callable("Fill") {
                    val self = it.checkSelf()
                    val (color, _) = it.checkColor(2)
                    self.pixmap.setColor(color)
                    self.pixmap.fill()
                    0
                }
                callable("CopyFrom") {
                    val self = it.checkSelf()
                    val sourceTexture = it.checkUserdata(2, LuaTexture::class)
                    val srcX = it.checkInt(3)
                    val srcY = it.checkInt(4)
                    val srcWidth = it.checkInt(5)
                    val srcHeight = it.checkInt(6)
                    val dstX = it.checkInt(7)
                    val dstY = it.checkInt(8)

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
                    0
                }
                callable("Update") {
                    val self = it.checkSelf()
                    self.texture.draw(self.pixmap, 0, 0)
                    0
                }
                callable("Dispose") {
                    val self = it.checkSelf()
                    self.texture.dispose()
                    self.pixmap.dispose()
                    0
                }
            }
        }
    }

    override fun register(table: LuaValue) {
        table.register("Create", this::luaCreateTexture)
    }

    /**
     * Creates a new editable texture with the specified dimensions and format.
     * Returns a `LuaTexture` object with pixel manipulation methods.
     *
     * Supported formats: "RGBA8888" (default), "RGB888", "RGBA4444", "RGB565", "Alpha"
     *
     * ```lua
     * LuaTexture Create(number width, number height)
     * LuaTexture Create(number width, number height, string format)
     * ```
     */
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

        val pixmap = Pixmap(width, height, format)
        val texture = Texture(pixmap)
        val luaTexture = LuaTexture(texture, pixmap)
        lua.push(luaTexture, Lua.Conversion.NONE)
        return 1
    }

}
