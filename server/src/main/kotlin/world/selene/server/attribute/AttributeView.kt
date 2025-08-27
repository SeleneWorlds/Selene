package world.selene.server.attribute

import party.iroiro.luajava.Lua
import world.selene.common.lua.LuaMappedMetatable
import world.selene.common.lua.LuaMetatable
import world.selene.common.lua.LuaMetatableProvider
import world.selene.common.lua.checkUserdata
import world.selene.common.lua.throwTypeError
import world.selene.common.lua.toAny

class AttributeView(val owner: Any, val name: String, val observer: AttributeViewObserver) : AttributeObserver,
    LuaMetatableProvider {
    val keysToAttributes = mutableMapOf<Any, Attribute<*>>()
    val attributesToKeys = mutableMapOf<Attribute<*>, Any>()

    fun getAttribute(key: Any): Attribute<*>? {
        return keysToAttributes[key]
    }

    fun addAttribute(key: Any, attribute: Attribute<*>) {
        attributesToKeys[attribute] = key
        keysToAttributes[key] = attribute
        attribute.observers.add(this)
    }

    fun dispose() {
        attributesToKeys.forEach { it.key.observers.remove(this) }
    }

    override fun attributeChanged(attribute: Attribute<*>) {
        val key = attributesToKeys[attribute]
            ?: throw IllegalStateException("Attribute $attribute is not registered in view $this")
        observer.attributeChanged(this, key, attribute)
    }

    override fun luaMetatable(lua: Lua): LuaMetatable {
        return luaMeta
    }

    companion object {
        val luaMeta = LuaMappedMetatable(AttributeView::class) {
            readOnly(AttributeView::owner)
            readOnly(AttributeView::name)
            callable("AddAttribute") { lua ->
                val view = lua.checkSelf()
                val key = lua.toAny(2) ?: lua.throwTypeError(2, Any::class)
                val attribute = lua.checkUserdata<Attribute<*>>(3)
                view.addAttribute(key, attribute)
                0
            }
            callable("GetAttribute") { lua ->
                val view = lua.checkSelf()
                val key = lua.toAny(2) ?: lua.throwTypeError(2, Any::class)
                val attribute = view.getAttribute(key)
                if (attribute != null) {
                    lua.push(attribute, Lua.Conversion.NONE)
                    return@callable 1
                }
                0
            }
        }
    }
}