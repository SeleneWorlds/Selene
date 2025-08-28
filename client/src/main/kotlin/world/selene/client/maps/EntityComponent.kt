package world.selene.client.maps

import com.badlogic.gdx.Gdx
import org.koin.mp.KoinPlatform.getKoin
import party.iroiro.luajava.Lua
import party.iroiro.luajava.value.LuaValue
import world.selene.common.data.ClientScriptComponentConfiguration
import world.selene.common.data.ComponentConfiguration
import world.selene.common.data.VisualComponentConfiguration
import world.selene.common.lua.LuaManager
import world.selene.common.lua.LuaMappedMetatable
import world.selene.common.lua.LuaMetatable
import world.selene.common.lua.LuaMetatableProvider
import world.selene.common.lua.newTable

interface EntityComponent {
    fun update(entity: Entity) = Unit
}

class VisualComponent(val configuration: VisualComponentConfiguration) : EntityComponent, LuaMetatableProvider {
    var red = 1f
    var green = 1f
    var blue = 1f
    var alpha = 1f

    override fun luaMetatable(lua: Lua): LuaMetatable {
        return luaMeta
    }

    companion object {
        val luaMeta = LuaMappedMetatable(VisualComponent::class) {
            writable(VisualComponent::red)
            writable(VisualComponent::green)
            writable(VisualComponent::blue)
            writable(VisualComponent::alpha)
        }
    }
}

class ClientScriptComponent(val configuration: ClientScriptComponentConfiguration) : EntityComponent {
    private val luaManager: LuaManager by getKoin().inject()
    private var module: LuaValue? = null
    private var data: LuaValue? = null

    override fun update(entity: Entity) {
        val lua = luaManager.lua
        val initPending = module == null
        val data = data ?: lua.newTable {}.also {
            data = it
        }
        lua.push(configuration.script)
        luaManager.luaRequire(lua)
        if (initPending) {
            lua.getField(-1, "Initialize")
            if (lua.isFunction(-1)) {
                lua.push(entity, Lua.Conversion.NONE)
                lua.push(data)
                lua.pCall(2, 0)
            } else lua.pop(1)
        }
        lua.getField(-1, "TickEntity")
        if (lua.isFunction(-1)) {
            lua.push(entity, Lua.Conversion.NONE)
            lua.push(data)
            lua.push(Gdx.graphics.deltaTime)
            lua.pCall(3, 0)
        } else lua.pop(1)
        lua.pop(1)
    }
}

fun ComponentConfiguration.create(): EntityComponent {
    return when (this) {
        is VisualComponentConfiguration -> VisualComponent(this)
        is ClientScriptComponentConfiguration -> ClientScriptComponent(this)
        else -> throw IllegalArgumentException("Unknown component configuration: $this")
    }
}