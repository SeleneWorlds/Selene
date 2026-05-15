package com.seleneworlds.common.grid

import kotlinx.serialization.Serializable

@Serializable
data class GridDefinition(
    val directions: List<GridDirectionDefinition>
)

@Serializable
data class GridDirectionDefinition(
    val name: String,
    val x: Int,
    val y: Int,
    val z: Int,
    val angle: Float
)
