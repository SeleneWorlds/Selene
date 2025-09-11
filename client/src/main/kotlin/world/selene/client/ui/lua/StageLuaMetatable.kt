package world.selene.client.ui.lua

import com.badlogic.gdx.scenes.scene2d.Stage
import party.iroiro.luajava.Lua
import world.selene.common.lua.LuaMappedMetatable
import world.selene.common.lua.util.checkFloat
import world.selene.common.lua.util.checkUserdata

@Suppress("SameReturnValue")
object StageLuaMetatable {
    val luaMeta = LuaMappedMetatable(Stage::class) {
        callable(::luaHit)
    }

    private fun luaHit(lua: Lua): Int {
        val stage = lua.checkUserdata<Stage>(1)
        val stageX = lua.checkFloat(2)
        val stageY = lua.checkFloat(3)
        val touchable = if(lua.isBoolean(4)) lua.toBoolean(4) else true
        val actor = stage.hit(stageX, stageY, touchable)
        lua.push(actor, Lua.Conversion.NONE)
        return 1
    }
}