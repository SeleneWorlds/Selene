@file:Suppress("unused")

package com.seleneworlds.client.rendering.visual

import com.badlogic.gdx.utils.Align
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import com.seleneworlds.common.data.MetadataHolder
import com.seleneworlds.common.data.RegistryAdoptedObject
import com.seleneworlds.common.serialization.SerializedMap
import com.seleneworlds.common.serialization.SerializedMapSerializer

@Serializable
sealed class VisualDefinition : MetadataHolder, RegistryAdoptedObject<VisualDefinition>()

@Serializable
@SerialName("simple")
data class SimpleVisualDefinition(
    val texture: String,
    val offsetX: Float = 0f,
    val offsetY: Float = 0f,
    val surfaceOffsetY: Float = 0f,
    val sortLayerOffset: Int = 0,
    val flipX: Boolean = false,
    val flipY: Boolean = false,
    @Serializable(with = SerializedMapSerializer::class)
    override val metadata: SerializedMap = emptyMap()
) : VisualDefinition()

@Serializable
@SerialName("variants")
data class VariantsVisualDefinition(
    val textures: List<String>,
    val offsetX: Float = 0f,
    val offsetY: Float = 0f,
    val surfaceOffsetY: Float = 0f,
    val sortLayerOffset: Int = 0,
    val flipX: Boolean = false,
    val flipY: Boolean = false,
    @Serializable(with = SerializedMapSerializer::class)
    override val metadata: SerializedMap = emptyMap()
) : VisualDefinition()

@Serializable
@SerialName("animated")
data class AnimatedVisualDefinition(
    val textures: List<String>,
    val duration: Float = 1f / 30f,
    val offsetX: Float = 0f,
    val offsetY: Float = 0f,
    val surfaceOffsetY: Float = 0f,
    val sortLayerOffset: Int = 0,
    val flipX: Boolean = false,
    val flipY: Boolean = false,
    val instanced: Boolean = false,
    @Serializable(with = SerializedMapSerializer::class)
    override val metadata: SerializedMap = emptyMap()
) : VisualDefinition()

@Serializable
@SerialName("animator")
data class AnimatorVisualDefinition(
    val animator: String,
    val animations: Map<String, AnimationFrames>,
    val offsetX: Float = 0f,
    val offsetY: Float = 0f,
    val surfaceOffsetY: Float = 0f,
    val sortLayerOffset: Int = 0,
    @Serializable(with = SerializedMapSerializer::class)
    override val metadata: SerializedMap = emptyMap()
) : VisualDefinition()

@Serializable
data class AnimationFrames(
    val textures: List<String>,
    val duration: Float? = null,
    val offsetX: Float? = null,
    val offsetY: Float? = null,
    val flipX: Boolean = false,
    val flipY: Boolean = false,
)

@Serializable
enum class HorizontalAlign(val align: Int) {
    LEFT(Align.left),
    CENTER(Align.center),
    RIGHT(Align.right)
}

@Serializable
@SerialName("text")
data class TextVisualDefinition(
    val text: String,
    val align: HorizontalAlign = HorizontalAlign.LEFT,
    @Serializable(with = SerializedMapSerializer::class)
    override val metadata: SerializedMap = emptyMap()
) : VisualDefinition()
