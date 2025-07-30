package world.selene.common.lua

import com.google.common.collect.HashBasedTable
import party.iroiro.luajava.value.LuaValue

class LuaMixinRegistry {
    private val typeMappings = mapOf(
        "Player" to "PlayerLuaProxy",
        "Entity" to "EntityLuaProxy"
    )
    private val table = HashBasedTable.create<String, String, LuaValue>()

    fun addMixin(typeName: String, functionName: String, function: LuaValue) {
        table.put(typeMappings[typeName] ?: typeName, functionName, function)
    }

    fun getMixin(typeName: String, functionName: String): LuaValue? {
        return table.get(typeName, functionName)
    }
}