package world.selene.server.attribute

import party.iroiro.luajava.Lua
import world.selene.common.lua.LuaMappedMetatable
import world.selene.common.lua.LuaMetatable
import world.selene.common.lua.LuaMetatableProvider
import world.selene.common.lua.ManagedLuaTable
import world.selene.common.lua.checkFunction
import world.selene.common.lua.checkUserdata
import world.selene.common.lua.throwTypeError
import world.selene.common.lua.toManagedTable

class Attribute<T : Any>(val owner: Any, val name: String) : LuaMetatableProvider {
    val observers = mutableListOf<Observer>()
    val filters = mutableListOf<Filter<T>>()

    var value: T? = null
        set(value) {
            val prev = field
            if (prev != value) {
                field = value
                observers.forEach { it.attributeChanged(this) }
            }
        }

    val effectiveValue: T?
        get() {
            var value = this.value
            filters.forEach {
                value = it.apply(this)
            }
            return value
        }

    override fun luaMetatable(lua: Lua): LuaMetatable {
        return luaMeta
    }

    companion object {
        val luaMeta = LuaMappedMetatable(Attribute::class) {
            writable(Attribute<*>::value)
            readOnly(Attribute<*>::effectiveValue)
            readOnly(Attribute<*>::owner)
            callable("AddObserver") { lua ->
                val attribute = lua.checkSelf()
                val observer = when (lua.type(2)) {
                    Lua.LuaType.FUNCTION -> LuaObserver(
                        lua.checkFunction(2),
                        lua.toManagedTable(3) ?: ManagedLuaTable()
                    )

                    Lua.LuaType.USERDATA -> lua.checkUserdata(2, Observer::class)
                    else -> lua.throwTypeError(2, Observer::class)
                }
                attribute.observers.add(observer)
                0
            }
            callable("RemoveObserver") { lua ->
                val attribute = lua.checkSelf()
                val observer = lua.checkUserdata(2, Observer::class)
                attribute.observers.remove(observer)
                0
            }
            callable("AddFilter") { lua ->
                val attribute = lua.checkSelf()
                val filter = lua.checkUserdata(2, Filter::class)
                @Suppress("UNCHECKED_CAST")
                (attribute.filters as MutableList<Filter<*>>).add(filter)
                0
            }
            callable("RemoveFilter") { lua ->
                val attribute = lua.checkSelf()
                val filter = lua.checkUserdata(2, Filter::class)
                attribute.filters.remove(filter)
                0
            }
        }
    }
}