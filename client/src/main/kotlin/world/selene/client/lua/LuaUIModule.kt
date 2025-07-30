package world.selene.client.lua

import party.iroiro.luajava.Lua
import party.iroiro.luajava.value.LuaValue
import world.selene.client.ui.Node
import world.selene.client.ui.NodeDefinition
import world.selene.client.ui.UI
import world.selene.common.lua.LuaManager
import world.selene.common.lua.LuaModule
import world.selene.common.lua.register

class LuaUIModule(private val ui: UI) : LuaModule {
    override val name = "selene.ui"

    override fun initialize(luaManager: LuaManager) {
        luaManager.exposeClass(Node.NodeLuaProxy::class)
    }

    override fun register(table: LuaValue) {
        table.register("LoadUI", this::luaLoadUI)
        table.register("CreateUI", this::luaCreateUI)
        table.set("Root", ui.customRoot.luaProxy)
        table.set("ANCHOR_TOP_LEFT", Node.Anchor.TOP_LEFT)
        table.set("ANCHOR_TOP", Node.Anchor.TOP)
        table.set("ANCHOR_TOP_RIGHT", Node.Anchor.TOP_RIGHT)
        table.set("ANCHOR_LEFT", Node.Anchor.LEFT)
        table.set("ANCHOR_CENTER", Node.Anchor.CENTER)
        table.set("ANCHOR_RIGHT", Node.Anchor.RIGHT)
        table.set("ANCHOR_BOTTOM_LEFT", Node.Anchor.BOTTOM_LEFT)
        table.set("ANCHOR_BOTTOM", Node.Anchor.BOTTOM)
        table.set("ANCHOR_BOTTOM_RIGHT", Node.Anchor.BOTTOM_RIGHT)
    }

    private fun luaCreateUI(lua: Lua): Int {
        val nodeDefinition = lua.toJavaObject(-1) as? NodeDefinition ?: return 0
        val node = ui.createNode(nodeDefinition)
        lua.push(node.luaProxy, Lua.Conversion.NONE)
        return 1
    }

    private fun luaLoadUI(lua: Lua): Int {
        // Assumes the Lua table is at the top of the stack
        fun parseNodeDefinition(idx: Int): NodeDefinition {
            // Get 'type'
            lua.getField(idx, "type")
            val type = lua.toString(-1) ?: ""
            lua.pop(1)

            // Get 'name'
            lua.getField(idx, "name")
            val name = lua.toString(-1) ?: ""
            lua.pop(1)

            // Get x and y
            var x = 0f
            var y = 0f
            lua.getField(idx, "x")
            if (lua.isNumber(-1)) {
                x = lua.toNumber(-1).toFloat()
            }
            lua.pop(1)
            lua.getField(idx, "y")
            if (lua.isNumber(-1)) {
                y = lua.toNumber(-1).toFloat()
            }
            lua.pop(1)

            // Get width and height
            var width = 0f
            var height = 0f
            lua.getField(idx, "width")
            if (lua.isNumber(-1)) {
                width = lua.toNumber(-1).toFloat()
            }
            lua.pop(1)
            lua.getField(idx, "height")
            if (lua.isNumber(-1)) {
                height = lua.toNumber(-1).toFloat()
            }
            lua.pop(1)

            // Get anchor
            var anchor = Node.Anchor.TOP_LEFT
            lua.getField(idx, "anchor")
            if (lua.isJavaObject(-1)) {
                anchor = lua.toJavaObject(-1) as? Node.Anchor ?: Node.Anchor.TOP_LEFT
            }
            lua.pop(1)

            // Get 'children'
            val children = mutableListOf<NodeDefinition>()
            lua.getField(idx, "children")
            if (lua.isTable(-1)) {
                lua.pushNil()
                while (lua.next(-2) != 0) {
                    // key at -2, value at -1
                    if (lua.isTable(-1)) {
                        children.add(parseNodeDefinition(lua.getTop()))
                    }
                    lua.pop(1)
                }
            }
            lua.pop(1)

            // Get 'attributes'
            val attributes = mutableMapOf<String, String>()
            lua.getField(idx, "attributes")
            if (lua.isTable(-1)) {
                lua.pushNil()
                while (lua.next(-2) != 0) {
                    val key = lua.toString(-2)
                    val value = lua.toString(-1)
                    if (key != null && value != null) {
                        attributes[key] = value
                    }
                    lua.pop(1)
                }
            }
            lua.pop(1)

            return NodeDefinition(type, name, x, y, width, height, anchor, children, attributes)
        }

        val nodeDefinition = parseNodeDefinition(-1)
        lua.pop(1) // Pop the input table

        lua.push(nodeDefinition, Lua.Conversion.NONE)
        return 1
    }
}