package world.selene.client.maps

import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.math.Rectangle
import org.koin.mp.KoinPlatform.getKoin
import party.iroiro.luajava.Lua
import party.iroiro.luajava.value.LuaValue
import world.selene.client.entity.component.IsoComponent
import world.selene.client.entity.component.RenderableComponent
import world.selene.client.entity.component.TickableComponent
import world.selene.client.rendering.visual2d.iso.IsoVisual
import world.selene.common.lua.LuaManager
import world.selene.common.lua.LuaMappedMetatable
import world.selene.common.lua.LuaMetatable
import world.selene.common.lua.LuaMetatableProvider
import world.selene.common.lua.LuaTrace
import world.selene.common.lua.newTable
import world.selene.common.lua.xpCall

interface EntityComponent

class IsoVisualComponent(val visual: IsoVisual) : EntityComponent, RenderableComponent, IsoComponent,
    LuaMetatableProvider {
    var red = 1f
    var green = 1f
    var blue = 1f
    var alpha = 1f

    override val sortLayerOffset: Int
        get() = visual.sortLayerOffset

    override val surfaceHeight: Float
        get() = visual.surfaceHeight

    override fun luaMetatable(lua: Lua): LuaMetatable {
        return luaMeta
    }

    override fun getBounds(x: Float, y: Float, outRect: Rectangle): Rectangle {
        return visual.getBounds(x, y, outRect)
    }

    override fun render(
        entity: Entity,
        batch: Batch,
        x: Float,
        y: Float
    ) {
        if (red != 1f || green != 1f || blue != 1f || alpha != 1f) {
            batch.color = batch.color.mul(red, green, blue, alpha)
        }
        visual.render(batch, x, y)
    }

    companion object {
        val luaMeta = LuaMappedMetatable(IsoVisualComponent::class) {
            readOnly(IsoVisualComponent::visual)
            writable(IsoVisualComponent::red)
            writable(IsoVisualComponent::green)
            writable(IsoVisualComponent::blue)
            writable(IsoVisualComponent::alpha)
        }
    }
}

class ClientScriptComponent(val moduleName: String) : EntityComponent, TickableComponent,
    LuaTrace {
    private val luaManager: LuaManager by getKoin().inject()
    private var module: LuaValue? = null
    private var data: LuaValue? = null

    override fun update(entity: Entity, delta: Float) {
        val lua = luaManager.lua
        val initPending = module == null
        val data = data ?: lua.newTable {}.also {
            data = it
        }
        lua.push(moduleName)
        luaManager.luaRequire(lua)
        if (initPending) {
            lua.getField(-1, "Initialize")
            if (lua.isFunction(-1)) {
                lua.push(entity, Lua.Conversion.NONE)
                lua.push(data)
                lua.xpCall(2, 0, this)
            } else lua.pop(1)
        }
        lua.getField(-1, "TickEntity")
        if (lua.isFunction(-1)) {
            lua.push(entity, Lua.Conversion.NONE)
            lua.push(data)
            lua.push(delta)
            lua.xpCall(3, 0, this)
        } else lua.pop(1)
        lua.pop(1)
    }

    override fun luaTrace(): String {
        return "[client script component \"${moduleName}\"]"
    }
}
