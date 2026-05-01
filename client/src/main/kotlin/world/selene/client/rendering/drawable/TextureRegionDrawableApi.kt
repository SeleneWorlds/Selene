package world.selene.client.rendering.drawable

import com.badlogic.gdx.graphics.g2d.TextureRegion
import party.iroiro.luajava.Lua
import world.selene.common.lua.LuaMetatable

class TextureRegionDrawableApi(val textureRegionDrawable: TextureRegionDrawable) : DrawableApi(textureRegionDrawable) {

    fun getTextureRegion(): TextureRegion {
        return textureRegionDrawable.textureRegion
    }

    fun withoutOffset(): TextureRegionDrawableApi {
        return textureRegionDrawable.withoutOffset().api
    }

    override fun luaMetatable(lua: Lua): LuaMetatable {
        return TextureRegionDrawableLuaApi.luaMeta
    }
}
