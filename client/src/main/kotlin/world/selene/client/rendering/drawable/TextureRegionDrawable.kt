package world.selene.client.rendering.drawable

import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Rectangle
import party.iroiro.luajava.Lua
import world.selene.common.lua.LuaMetatable
import world.selene.common.lua.checkUserdata

class TextureRegionDrawable(val textureRegion: TextureRegion, val offsetX: Float, val offsetY: Float) : Drawable {
    override fun update(delta: Float) = Unit
    override fun render(batch: Batch, x: Float, y: Float) {
        batch.draw(textureRegion, x + offsetX, y + offsetY)
    }

    override fun render(
        batch: Batch,
        x: Float,
        y: Float,
        width: Float,
        height: Float
    ) {
        batch.draw(textureRegion, x + offsetX, y + offsetY, width, height)
    }

    override fun render(
        batch: Batch, x: Float, y: Float,
        originX: Float, originY: Float,
        width: Float, height: Float,
        scaleX: Float, scaleY: Float,
        rotation: Float
    ) {
        batch.draw(textureRegion, x + offsetX, y + offsetY, originX, originY, width, height, scaleX, scaleY, rotation)
    }

    override fun getBounds(x: Float, y: Float, outRect: Rectangle): Rectangle {
        outRect.set(
            x + offsetX,
            y + offsetY,
            textureRegion.regionWidth.toFloat(),
            textureRegion.regionHeight.toFloat()
        )
        return outRect
    }

    override fun luaMetatable(lua: Lua): LuaMetatable {
        return luaMeta
    }

    fun withoutOffset(): TextureRegionDrawable {
        return TextureRegionDrawable(textureRegion, 0f, 0f)
    }

    @Suppress("SameReturnValue")
    companion object {
        /**
         * Texture region rendered by this drawable.
         *
         * ```property
         * TextureRegion: TextureRegion
         * ```
         */
        private fun luaGetTextureRegion(lua: Lua): Int {
            val self = lua.checkUserdata<TextureRegionDrawable>(1)
            lua.push(self.textureRegion, Lua.Conversion.NONE)
            return 1
        }

        /**
         * Gets a new drawable without any offset.
         *
         * ```signatures
         * WithoutOffset() -> TextureRegionDrawable
         * ```
         */
        private fun luaWithoutOffset(lua: Lua): Int {
            val self = lua.checkUserdata<TextureRegionDrawable>(1)
            lua.push(self.withoutOffset(), Lua.Conversion.NONE)
            return 1
        }

        val luaMeta = Drawable.luaMeta.extend(TextureRegionDrawable::class) {
            getter(::luaGetTextureRegion)
            callable(::luaWithoutOffset)
        }
    }
}
