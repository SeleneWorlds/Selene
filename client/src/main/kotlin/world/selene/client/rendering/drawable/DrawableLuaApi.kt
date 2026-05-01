package world.selene.client.rendering.drawable

import world.selene.common.lua.LuaMappedMetatable

object DrawableLuaApi {
    val luaMeta = LuaMappedMetatable(DrawableApi::class) {
    }
}
