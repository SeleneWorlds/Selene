package world.selene.client.ui.lua

import party.iroiro.luajava.value.LuaValue
import world.selene.common.lua.LuaManager
import world.selene.common.lua.LuaModule
import world.selene.common.lua.util.register

/**
 * Load, skin and manipulate UIs.
 */
class UILuaApi(
    private val api: UIApi
) : LuaModule {
    override val name = "selene.ui.lml"

    override fun initialize(luaManager: LuaManager) {
        api.initialize(luaManager)
    }

    override fun register(table: LuaValue) {
        table.register("LoadUI", api::luaLoadUI)
        table.register("LoadSkin", api::luaLoadSkin)
        table.register("CreateSkin", api::luaCreateSkin)
        table.register("CreateContainer", api::luaCreateContainer)
        table.register("CreateLabel", api::luaCreateLabel)
        table.register("AddToRoot", api::luaAddToRoot)
        table.register("SetFocus", api::luaSetFocus)
        table.register("GetFocus", api::luaGetFocus)
        table.register("CreateImageButtonStyle", api::luaCreateImageButtonStyle)
        table.register("CreateButtonStyle", api::luaCreateButtonStyle)
        table.register("AddInputProcessor", api::luaAddInputProcessor)
        table.register("CreateDragListener", api::luaCreateDragListener)
        table.set("Root", api.bundlesRoot)
    }
}
