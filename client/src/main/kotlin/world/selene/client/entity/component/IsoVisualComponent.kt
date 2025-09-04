package world.selene.client.entity.component

import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.math.Rectangle
import party.iroiro.luajava.Lua
import world.selene.client.maps.Entity
import world.selene.client.rendering.visual2d.iso.IsoVisual
import world.selene.common.lua.LuaMappedMetatable
import world.selene.common.lua.LuaMetatable
import world.selene.common.lua.LuaMetatableProvider
import world.selene.common.lua.checkFloat
import world.selene.common.lua.checkUserdata

class IsoVisualComponent(val visual: IsoVisual, override val positioner: ComponentPositioner) : EntityComponent,
    RenderableComponent, IsoComponent,
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

    override fun toString(): String {
        return "IsoVisualComponent(visual=$visual)"
    }

    companion object {
        /**
         * Gets the visual of this component.
         *
         * ```property
         * Visual: IsoVisual
         * ```
         */
        private fun luaGetVisual(lua: Lua): Int {
            val component = lua.checkUserdata<IsoVisualComponent>(1)
            lua.push(component.visual, Lua.Conversion.NONE)
            return 1
        }

        /**
         * Gets the red color of this component.
         *
         * ```property
         * Red: number
         * ```
         */
        private fun luaGetRed(lua: Lua): Int {
            val component = lua.checkUserdata<IsoVisualComponent>(1)
            lua.push(component.red)
            return 1
        }

        /**
         * Sets the red color of this component.
         *
         * ```property
         * Red: number
         * ```
         */
        private fun luaSetRed(lua: Lua): Int {
            val component = lua.checkUserdata<IsoVisualComponent>(1)
            component.red = lua.checkFloat(3)
            return 0
        }

        /**
         * Gets the green color of this component.
         *
         * ```property
         * Green: number
         * ```
         */
        private fun luaGetGreen(lua: Lua): Int {
            val component = lua.checkUserdata<IsoVisualComponent>(1)
            lua.push(component.green)
            return 1
        }

        /**
         * Sets the green color of this component.
         *
         * ```property
         * Green: number
         * ```
         */
        private fun luaSetGreen(lua: Lua): Int {
            val component = lua.checkUserdata<IsoVisualComponent>(1)
            component.green = lua.checkFloat(3)
            return 0
        }

        /**
         * Gets the blue color of this component.
         *
         * ```property
         * Blue: number
         * ```
         */
        private fun luaGetBlue(lua: Lua): Int {
            val component = lua.checkUserdata<IsoVisualComponent>(1)
            lua.push(component.blue)
            return 1
        }

        /**
         * Sets the blue color of this component.
         *
         * ```property
         * Blue: number
         * ```
         */
        private fun luaSetBlue(lua: Lua): Int {
            val component = lua.checkUserdata<IsoVisualComponent>(1)
            component.blue = lua.checkFloat(3)
            return 0
        }

        /**
         * Gets the alpha color of this component.
         *
         * ```property
         * Alpha: number
         * ```
         */
        private fun luaGetAlpha(lua: Lua): Int {
            val component = lua.checkUserdata<IsoVisualComponent>(1)
            lua.push(component.alpha)
            return 0
        }

        /**
         * Sets the alpha color of this component.
         *
         * ```property
         * Alpha: number
         * ```
         */
        private fun luaSetAlpha(lua: Lua): Int {
            val component = lua.checkUserdata<IsoVisualComponent>(1)
            component.alpha = lua.checkFloat(3)
            return 1
        }

        val luaMeta = LuaMappedMetatable(IsoVisualComponent::class) {
            getter(::luaGetVisual)
            getter(::luaGetRed)
            setter(::luaSetRed)
            getter(::luaGetGreen)
            setter(::luaSetGreen)
            getter(::luaGetBlue)
            setter(::luaSetBlue)
            getter(::luaGetAlpha)
            setter(::luaSetAlpha)
        }
    }
}