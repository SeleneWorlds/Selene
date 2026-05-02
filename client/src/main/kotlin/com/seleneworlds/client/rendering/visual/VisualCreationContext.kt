package com.seleneworlds.client.rendering.visual

import com.seleneworlds.client.rendering.animator.AnimatorController
import com.seleneworlds.common.grid.Coordinate
import com.seleneworlds.common.serialization.SerializedMap

data class VisualCreationContext(
    val coordinate: Coordinate = Coordinate.Zero,
    var animatorController: AnimatorController? = null,
    val overrides: SerializedMap = emptyMap()
)
