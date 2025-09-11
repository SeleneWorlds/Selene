package world.selene.client.entity.component.rendering

import com.badlogic.gdx.math.Vector2
import world.selene.client.entity.Entity
import world.selene.common.entities.VisualComponentPosition

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

        override fun toString(): String {
            return "None(offsetX=$offsetX, offsetY=$offsetY)"
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

        override fun toString(): String {
            return "Top(offsetX=$offsetX, offsetY=$offsetY)"
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