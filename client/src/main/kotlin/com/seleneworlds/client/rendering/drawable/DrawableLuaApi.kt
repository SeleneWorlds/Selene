package com.seleneworlds.client.rendering.drawable

import com.seleneworlds.common.lua.LuaMappedMetatable

object DrawableLuaApi {
    val luaMeta = LuaMappedMetatable(DrawableApi::class) {
    }
}
