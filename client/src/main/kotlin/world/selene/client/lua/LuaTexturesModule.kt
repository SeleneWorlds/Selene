package world.selene.client.lua

import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import party.iroiro.luajava.Lua
import party.iroiro.luajava.value.LuaValue
import world.selene.common.lua.LuaModule
import world.selene.common.lua.checkInt
import world.selene.common.lua.checkString
import world.selene.common.lua.register

/**
 * Create and manipulate textures.
 */
@Suppress("SameReturnValue")
class LuaTexturesModule : LuaModule {
    override val name = "selene.textures"

    override fun register(table: LuaValue) {
        table.register("Create", this::luaCreateTexture)
    }

    /**
     * Creates a new editable texture with the specified dimensions and format.
     * Returns a `LuaTexture` object with pixel manipulation methods.
     *
     * Supported formats: "RGBA8888" (default), "RGB888", "RGBA4444", "RGB565", "Alpha"
     *
     * ```signatures
     * Create(width: number, height: number) -> LuaTexture
     * Create(width: number, height: number, format: string) -> LuaTexture
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
