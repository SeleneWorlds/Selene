package com.seleneworlds.common.tiles

import kotlinx.serialization.Serializable
import com.seleneworlds.common.data.Identifier
import com.seleneworlds.common.data.MetadataHolder
import com.seleneworlds.common.data.RegistryAdoptedObject
import com.seleneworlds.common.data.TagHolder
import com.seleneworlds.common.serialization.SerializedMap
import com.seleneworlds.common.serialization.SerializedMapSerializer

@Serializable
data class TileDefinition(
    val visual: Identifier,
    val impassable: Boolean = false,
    val passableAbove: Boolean = false,
    val movementDuration: Float = DEFAULT_MOVEMENT_DURATION,
    val light: TileLight? = null,
    @Serializable(with = SerializedMapSerializer::class)
    override val metadata: SerializedMap = emptyMap(),
    override val tags: Set<String> = emptySet(),
    val mapColor: String = "#ffffff"
) : MetadataHolder, TagHolder, RegistryAdoptedObject<TileDefinition>() {
    init {
        require(Regex("#[0-9a-fA-F]{6}").matches(mapColor)) { "mapColor must be a #RRGGBB color" }
        require(movementDuration.isFinite() && movementDuration >= 0f) {
            "movementDuration must be a finite, non-negative number"
        }
    }

    companion object {
        const val DEFAULT_MOVEMENT_DURATION = 0.2f
    }
}

@Serializable
data class TileLight(
    val radius: Float,
    val intensity: Float = 1f,
    val red: Float = 1f,
    val green: Float = 1f,
    val blue: Float = 1f,
) {
    init {
        require(radius.isFinite() && radius > 0f) { "radius must be a finite, positive number" }
        require(intensity.isFinite() && intensity >= 0f) { "intensity must be a finite, non-negative number" }
        require(red.isFinite() && green.isFinite() && blue.isFinite()) { "light colors must be finite numbers" }
    }
}
