package world.selene.client.entity.component

import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.math.Rectangle
import party.iroiro.luajava.Lua
import world.selene.client.maps.Entity
import world.selene.client.rendering.visual2d.Visual2D
import world.selene.common.lua.LuaMappedMetatable
import world.selene.common.lua.LuaMetatable
import world.selene.common.lua.LuaMetatableProvider

class Visual2DComponent(val visual: Visual2D, override val positioner: ComponentPositioner) : EntityComponent, RenderableComponent, LuaMetatableProvider {
    var red = 1f
    var green = 1f
    var blue = 1f
    var alpha = 1f

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