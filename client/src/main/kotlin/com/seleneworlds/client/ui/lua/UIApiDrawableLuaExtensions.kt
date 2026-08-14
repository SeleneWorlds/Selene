package com.seleneworlds.client.ui.lua

import com.badlogic.gdx.scenes.scene2d.utils.Drawable
import com.seleneworlds.client.rendering.drawable.DrawableApi
import com.seleneworlds.client.rendering.visual2d.Visual2DApi
import com.seleneworlds.client.ui.ThemeApi
import com.seleneworlds.client.ui.UIApi
import com.seleneworlds.client.ui.drawable.DrawableDrawable
import com.seleneworlds.client.ui.drawable.Visual2DDrawable
import com.seleneworlds.common.lua.util.checkString
import com.seleneworlds.common.lua.util.checkUserdata
import com.seleneworlds.common.lua.util.getField
import com.seleneworlds.common.lua.util.throwError
import party.iroiro.luajava.Lua

fun Lua.getFieldDrawable(
    api: UIApi,
    tableIndex: Int,
    fieldName: String,
    theme: ThemeApi?
): Drawable? {
    return getField(tableIndex, fieldName) { type ->
        when (type) {
            Lua.LuaType.STRING -> theme?.let { api.resolveDrawable(it, toString(-1)!!) }
            Lua.LuaType.USERDATA -> toDrawable(-1)
            else -> null
        }
    }
}

fun Lua.checkDrawable(
    index: Int
): Drawable {
    return toDrawable(index)
        ?: throwError("Expected drawable key, Visual2D, or Drawable at index $index")
}

fun Lua.checkDrawable(
    api: UIApi,
    themeIndex: Int,
    keyIndex: Int
): Drawable {
    val theme = checkUserdata<ThemeApi>(themeIndex)
    val key = checkString(keyIndex)
    return api.resolveDrawable(theme, key)
        ?: throwError("Drawable not found in theme: $key")
}

private fun Lua.toDrawable(index: Int): Drawable? {
    return when (type(index)) {
        Lua.LuaType.USERDATA -> {
            when (val value = toJavaObject(index)) {
                is Visual2DApi -> Visual2DDrawable(value.delegate)
                is DrawableApi -> DrawableDrawable(value.drawable)
                else -> null
            }
        }

        else -> null
    }
}
