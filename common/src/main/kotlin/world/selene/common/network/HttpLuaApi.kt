package world.selene.common.network

import party.iroiro.luajava.value.LuaValue
import world.selene.common.lua.LuaModule
import world.selene.common.lua.util.register
import world.selene.common.util.Disposable

/**
 * Make HTTP web requests.
 */
class HttpLuaApi(private val api: HttpApi) : LuaModule, Disposable {
    override val name = "selene.http"

    override fun register(table: LuaValue) {
        table.register("Post", api::luaPost)
    }

    override fun dispose() {
        api.dispose()
    }
}
