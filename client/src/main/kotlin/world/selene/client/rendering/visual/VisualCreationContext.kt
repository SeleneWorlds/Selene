package world.selene.client.rendering.visual

import world.selene.client.rendering.animator.AnimatorController
import world.selene.common.util.Coordinate

data class VisualCreationContext(
    val coordinate: Coordinate,
    var animatorController: AnimatorController? = null
)
