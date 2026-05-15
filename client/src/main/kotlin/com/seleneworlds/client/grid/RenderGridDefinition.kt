package com.seleneworlds.client.grid

import kotlinx.serialization.Serializable

@Serializable
data class RenderGridDefinition(
    val tileWidth: Int = 76,
    val tileHeight: Int = 37,
    val tileStepX: Float = 38f,
    val tileStepY: Float = 19f,
    val tileStepZ: Float = 114f,
    val zSortScale: Int = 500,
    val rowSortScale: Int = 50
)
