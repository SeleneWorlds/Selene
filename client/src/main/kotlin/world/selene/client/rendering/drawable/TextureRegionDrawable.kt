package world.selene.client.rendering.drawable

import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Rectangle
import party.iroiro.luajava.Lua
import world.selene.common.lua.LuaMetatable

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

    companion object {
        val luaMeta = Drawable.luaMeta.extend(TextureRegionDrawable::class) {
            readOnly(TextureRegionDrawable::textureRegion)
            callable(TextureRegionDrawable::withoutOffset)
        }
    }
}
