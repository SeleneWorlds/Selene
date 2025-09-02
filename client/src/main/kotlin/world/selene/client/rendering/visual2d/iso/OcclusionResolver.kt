package world.selene.client.rendering.visual2d.iso

import com.badlogic.gdx.math.Rectangle
import world.selene.client.grid.ClientGrid
import world.selene.client.rendering.drawable.Drawable
import world.selene.common.util.Coordinate

class OcclusionResolver(private val grid: ClientGrid) {
    fun occludes(
        focusCoordinate: Coordinate, focusDrawable: Drawable?, focusX: Float, focusY: Float,
        occluderCoordinate: Coordinate, occluderDrawable: Drawable, occluderX: Float, occluderY: Float
    ): Boolean {
        val thisSortLayer = grid.getSortLayer(occluderCoordinate, 0)
        val focusSortLayer = grid.getSortLayer(focusCoordinate, 0)
        if (focusSortLayer - thisSortLayer < grid.rowSortScale) {
            return false
        }

        val thisBounds = occluderDrawable.getBounds(occluderX, occluderY, Rectangle())
        val focusBounds = focusDrawable?.getBounds(focusX, focusY, Rectangle()) ?: run {
            Rectangle(grid.getScreenX(focusCoordinate), grid.getScreenY(focusCoordinate), 1f, 1f)
        }

        val isLargerThanFocus = thisBounds.height >= focusBounds.height
        val isAboveFocus = focusCoordinate.z < occluderCoordinate.z
        return (isLargerThanFocus || isAboveFocus) && thisBounds.overlaps(focusBounds)
    }
}