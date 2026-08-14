package com.seleneworlds.client.ui.lua

import com.badlogic.gdx.scenes.scene2d.ui.Image
import party.iroiro.luajava.Lua
import com.seleneworlds.client.ui.UIApi
import com.seleneworlds.common.lua.util.checkUserdata

class ImageLuaMetatable(private val api: UIApi) {
    val luaMeta = ActorLuaMetatable.luaMeta.extend(Image::class) {
        callable(::setDrawable)
    }

    /**
     * Sets the image drawable from a theme key, Visual2D, or Drawable.
     *
     * ```signatures
     * SetDrawable(drawable: Visual2D|Drawable)
     * SetDrawable(theme: ThemeApi, drawableKey: string)
     * ```
     */
    private fun setDrawable(lua: Lua): Int {
        val actor = lua.checkUserdata<Image>(1)
        actor.drawable = if (lua.isUserdata(2) && lua.isString(3)) {
            lua.checkDrawable(api, 2, 3)
        } else {
            lua.checkDrawable(2)
        }
        return 0
    }
}
