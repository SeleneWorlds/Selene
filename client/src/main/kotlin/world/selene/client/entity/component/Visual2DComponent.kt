package world.selene.client.entity.component

import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.math.Rectangle
import party.iroiro.luajava.Lua
import world.selene.client.maps.Entity
import world.selene.client.rendering.visual2d.Visual2D
import world.selene.common.lua.LuaMappedMetatable
import world.selene.common.lua.LuaMetatable
import world.selene.common.lua.LuaMetatableProvider
import world.selene.common.lua.checkFloat
import world.selene.common.lua.checkUserdata

class Visual2DComponent(val visual: Visual2D, override val positioner: ComponentPositioner) : EntityComponent,
    RenderableComponent, LuaMetatableProvider {
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

    override fun toString(): String {
        return "Visual2DComponent(visual=${visual})"
    }

    companion object {
        /**
         * Gets the visual of this component.
         *
         * ```property
         * Visual: Visual2D
         * ```
         */
        private fun luaGetVisual(lua: Lua): Int {
            val component = lua.checkUserdata<Visual2DComponent>(1)
            lua.push(component.visual, Lua.Conversion.NONE)
            return 1
        }

        /**
         * Gets the red component of the color of this component.
         *
         * ```property
         * Red: number
         * ```
         */
        private fun luaGetRed(lua: Lua): Int {
            val component = lua.checkUserdata<Visual2DComponent>(1)
            lua.push(component.red)
            return 1
        }

        /**
         * Sets the red component of the color of this component.
         *
         * ```property
         * Red: number
         * ```
         */
        private fun luaSetRed(lua: Lua): Int {
            val component = lua.checkUserdata<Visual2DComponent>(1)
            component.red = lua.checkFloat(3)
            return 0
        }

        /**
         * Gets the green component of the color of this component.
         *
         * ```property
         * Green: number
         * ```
         */
        private fun luaGetGreen(lua: Lua): Int {
            val component = lua.checkUserdata<Visual2DComponent>(1)
            lua.push(component.green)
            return 1
        }

        /**
         * Sets the green component of the color of this component.
         *
         * ```property
         * Green: number
         * ```
         */
        private fun luaSetGreen(lua: Lua): Int {
            val component = lua.checkUserdata<Visual2DComponent>(1)
            component.green = lua.checkFloat(3)
            return 0
        }

        /**
         * Gets the blue component of the color of this component.
         *
         * ```property
         * Blue: number
         * ```
         */
        private fun luaGetBlue(lua: Lua): Int {
            val component = lua.checkUserdata<Visual2DComponent>(1)
            lua.push(component.blue)
            return 1
        }

        /**
         * Sets the blue component of the color of this component.
         *
         * ```property
         * Blue: number
         * ```
         */
        private fun luaSetBlue(lua: Lua): Int {
            val component = lua.checkUserdata<Visual2DComponent>(1)
            component.blue = lua.checkFloat(3)
            return 0
        }

        /**
         * Gets the alpha component of the color of this component.
         *
         * ```property
         * Alpha: number
         * ```
         */
        private fun luaGetAlpha(lua: Lua): Int {
            val component = lua.checkUserdata<Visual2DComponent>(1)
            lua.push(component.alpha)
            return 0
        }

        /**
         * Sets the alpha component of the color of this component.
         *
         * ```property
         * Alpha: number
         * ```
         */
        private fun luaSetAlpha(lua: Lua): Int {
            val component = lua.checkUserdata<Visual2DComponent>(1)
            component.alpha = lua.checkFloat(3)
            return 1
        }

        val luaMeta = LuaMappedMetatable(Visual2DComponent::class) {
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