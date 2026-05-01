package world.selene.client.rendering.lua

import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import party.iroiro.luajava.Lua
import world.selene.common.lua.util.checkInt
import world.selene.common.lua.util.checkString

/**
 * Create and manipulate textures.
 */
@Suppress("SameReturnValue")
class TexturesApi {
    fun luaCreateTexture(lua: Lua): Int {
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
        val scriptableTexture = ScriptableTexture(texture, pixmap)
        lua.push(scriptableTexture, Lua.Conversion.NONE)
        return 1
    }
}
