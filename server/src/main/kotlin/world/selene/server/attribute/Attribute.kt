package world.selene.server.attribute

import party.iroiro.luajava.Lua
import world.selene.common.lua.LuaMappedMetatable
import world.selene.common.lua.LuaMetatable
import world.selene.common.lua.LuaMetatableProvider
import world.selene.common.lua.checkFunction
import world.selene.common.lua.checkString
import world.selene.common.lua.checkUserdata
import world.selene.common.lua.throwTypeError
import world.selene.common.lua.toAny

class Attribute<T : Any?>(val owner: Any, val name: String, initialValue: T) : LuaMetatableProvider {
    val observers = mutableListOf<AttributeObserver>()
    val constraints = mutableListOf<AttributeFilter<T>>()
    val constraintsByName = mutableMapOf<String, AttributeFilter<T>>()
    val modifiers = mutableListOf<AttributeFilter<T>>()
    val modifiersByName = mutableMapOf<String, AttributeFilter<T>>()

    var value: T = initialValue
        set(value) {
            val prev = field
            if (prev != value) {
                field = value
                constraints.forEach {
                    if (it.enabled) {
                        field = it.apply(this, field)
                    }
                }
                notifyObservers()
            }
        }

    val effectiveValue: T
        get() {
            var value = this.value
            modifiers.forEach {
                if (it.enabled) {
                    value = it.apply(this, value)
                }
            }
            return value
        }

    private fun notifyObservers() {
        observers.forEach { it.attributeChanged(this) }
    }

    override fun luaMetatable(lua: Lua): LuaMetatable {
        return luaMeta
    }

    override fun toString(): String {
        return "Attribute($owner.$name = $value)"
    }

    companion object {
        val luaMeta = LuaMappedMetatable(Attribute::class) {
            readOnly(Attribute<*>::name)
            writable(Attribute<*>::value)
            readOnly(Attribute<*>::effectiveValue)
            readOnly(Attribute<*>::owner)
            callable("AddObserver") { lua ->
                val attribute = lua.checkSelf()
                val observer = when (lua.type(2)) {
                    Lua.LuaType.FUNCTION -> LuaAttributeObserver(lua.checkFunction(2))
                    Lua.LuaType.USERDATA -> lua.checkUserdata<AttributeObserver>(2)
                    else -> lua.throwTypeError(2, AttributeObserver::class)
                }
                attribute.observers.add(observer)
                lua.push(observer, Lua.Conversion.NONE)
                1
            }
            callable("RemoveObserver") { lua ->
                val attribute = lua.checkSelf()
                val observer = lua.checkUserdata(2, AttributeObserver::class)
                attribute.observers.remove(observer)
                0
            }
            callable("AddConstraint") { lua ->
                @Suppress("UNCHECKED_CAST")
                val attribute = lua.checkSelf() as Attribute<Any?>
                val filter = when (lua.type(3)) {
                    Lua.LuaType.FUNCTION -> LuaAttributeFilter(
                        lua.checkFunction(3),
                        lua.toAny(4)
                    )

                    Lua.LuaType.USERDATA -> lua.checkUserdata<AttributeFilter<Any?>>(2)
                    else -> lua.throwTypeError(2, AttributeFilter::class)
                }
                attribute.addConstraint(lua.checkString(2), filter)
                lua.push(filter, Lua.Conversion.NONE)
                1
            }
            callable("RemoveConstraint") { lua ->
                val attribute = lua.checkSelf()
                attribute.removeConstraint(lua.checkString(2))
                0
            }
            callable("AddModifier") { lua ->
                @Suppress("UNCHECKED_CAST")
                val attribute = lua.checkSelf() as Attribute<Any?>
                val filter = when (lua.type(3)) {
                    Lua.LuaType.FUNCTION -> LuaAttributeFilter(
                        lua.checkFunction(3),
                        lua.toAny(4)
                    )

                    Lua.LuaType.USERDATA -> lua.checkUserdata<AttributeFilter<Any?>>(2)
                    else -> lua.throwTypeError(2, AttributeFilter::class)
                }
                attribute.addModifier(lua.checkString(2), filter)
                lua.push(filter, Lua.Conversion.NONE)
                1
            }
            callable("RemoveModifier") { lua ->
                val attribute = lua.checkSelf()
                attribute.removeModifier(lua.checkString(2))
                0
            }
            callable("Refresh") { lua ->
                val attribute = lua.checkSelf()
                attribute.notifyObservers()
                0
            }
        }
    }

    private fun addModifier(name: String, filter: AttributeFilter<T>) {
        modifiersByName.put(name, filter)?.let {
            modifiers.remove(it)
        }
        modifiers.add(filter)
    }

    private fun removeModifier(name: String) {
        modifiersByName.remove(name)?.let {
            modifiers.remove(it)
        }
    }

    private fun addConstraint(name: String, filter: AttributeFilter<T>) {
        constraintsByName.put(name, filter)?.let {
            constraints.remove(it)
        }
        constraints.add(filter)
    }

    private fun removeConstraint(name: String) {
        constraintsByName.remove(name)?.let {
            constraints.remove(it)
        }
    }
}