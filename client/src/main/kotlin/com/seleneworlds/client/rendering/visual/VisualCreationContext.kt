package com.seleneworlds.client.rendering.visual

import com.seleneworlds.client.rendering.animator.AnimatorController
import com.seleneworlds.common.grid.Coordinate

data class VisualCreationContext(
    val coordinate: Coordinate = Coordinate.Zero,
    var animatorController: AnimatorController? = null,
    val overrides: Map<String, Any> = emptyMap()
)
