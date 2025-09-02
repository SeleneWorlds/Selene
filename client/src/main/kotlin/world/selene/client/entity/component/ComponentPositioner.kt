package world.selene.client.entity.component

import com.badlogic.gdx.math.Vector2
import world.selene.client.maps.Entity
import world.selene.common.data.VisualComponentPosition

sealed interface ComponentPositioner {
    fun applyPositioning(entity: Entity, outVec: Vector2): Vector2

    class None(val offsetX: Float, val offsetY: Float) : ComponentPositioner {
        override fun applyPositioning(
            entity: Entity,
            outVec: Vector2
        ): Vector2 {
            return outVec.apply {
                x += offsetX
                y += offsetY
            }
        }
    }

    class Top(val offsetX: Float, val offsetY: Float) : ComponentPositioner {
        override fun applyPositioning(
            entity: Entity,
            outVec: Vector2
        ): Vector2 {
            return outVec.apply {
                x += offsetX
                y = entity.lastRenderBounds.y + entity.lastRenderBounds.height + offsetY
            }
        }
    }

    companion object {
        fun of(position: VisualComponentPosition): ComponentPositioner {
            return when (position.origin.lowercase()) {
                "top" -> Top(position.offsetX, position.offsetY)
                else -> None(position.offsetX, position.offsetY)
            }
        }
    }
}