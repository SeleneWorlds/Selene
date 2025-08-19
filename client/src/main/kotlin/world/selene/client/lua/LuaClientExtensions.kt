package world.selene.client.lua

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Colors
import party.iroiro.luajava.Lua
import world.selene.common.lua.checkFloat
import world.selene.common.lua.checkJavaObject
import world.selene.common.lua.getFieldFloat

fun Lua.checkColor(index: Int): Pair<Color, Int> {
    return when (type(index)) {
        Lua.LuaType.TABLE -> {
            val r = getFieldFloat(index, "r") ?: getFieldFloat(index, "red") ?: 1f
            val g = getFieldFloat(index, "g") ?: getFieldFloat(index, "green") ?: 1f
            val b = getFieldFloat(index, "b") ?: getFieldFloat(index, "blue") ?: 1f
            val a = getFieldFloat(index, "a") ?: getFieldFloat(index, "alpha") ?: 1f
            return Color(r, g, b, a) to index
        }

        Lua.LuaType.USERDATA -> checkJavaObject(index, Color::class) to index

        Lua.LuaType.STRING -> {
            val colorString = toString(index)!!
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
                    Colors.get(colorString.uppercase())
                        ?: throw IllegalArgumentException("Unknown color name: $colorString")
                }
            } to index
        }

        else -> Color(
            checkFloat(index),
            checkFloat(index + 1),
            checkFloat(index + 2),
            checkFloat(index + 3)
        ) to index + 3
    }
}