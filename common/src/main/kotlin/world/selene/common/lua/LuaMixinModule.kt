package world.selene.common.lua

import party.iroiro.luajava.Lua
import party.iroiro.luajava.value.LuaValue

class LuaMixinModule(private val mixinRegistry: LuaMixinRegistry) : LuaModule {
    override val name = "selene.mixins"

    override fun register(table: LuaValue) {
        table.register("Mixin", this::luaMixin)
    }

    private fun luaMixin(lua: Lua, args: Array<LuaValue>): Array<LuaValue> {
        val typeName = args[0].toString()
        val functionName = args[1].toString()
        val function = args[2]
        mixinRegistry.addMixin(typeName, functionName, function)
        return emptyArray()
    }
}