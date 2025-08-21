package world.selene.client.data

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import world.selene.common.data.MetadataHolder

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes(
    JsonSubTypes.Type(value = SimpleVisualDefinition::class, name = "simple"),
    JsonSubTypes.Type(value = VariantsVisualDefinition::class, name = "variants"),
    JsonSubTypes.Type(value = AnimatedVisualDefinition::class, name = "animated"),
    JsonSubTypes.Type(value = AnimatorVisualDefinition::class, name = "animator"),
    JsonSubTypes.Type(value = LabelVisualDefinition::class, name = "label")
)
interface VisualDefinition {
    @get:JsonIgnore
    val isShared: Boolean get() = true
}

data class SimpleVisualDefinition(
    val texture: String,
    val offsetX: Int = 0,
    val offsetY: Int = 0,
    val surfaceOffsetY: Int = 0,
    val sortLayerOffset: Int = 0,
    override val metadata: Map<String, Any> = emptyMap()
) : VisualDefinition, MetadataHolder

data class VariantsVisualDefinition(
    val textures: List<String>,
    val offsetX: Int = 0,
    val offsetY: Int = 0,
    val surfaceOffsetY: Int = 0,
    val sortLayerOffset: Int = 0,
    override val metadata: Map<String, Any> = emptyMap()
) : VisualDefinition, MetadataHolder

data class AnimatedVisualDefinition(
    val textures: List<String>,
    val duration: Float = 0.1f,
    val offsetX: Int = 0,
    val offsetY: Int = 0,
    val surfaceOffsetY: Int = 0,
    val sortLayerOffset: Int = 0,
    override val metadata: Map<String, Any> = emptyMap(),
    val instance: Boolean = false
) : VisualDefinition, MetadataHolder {
    override val isShared: Boolean
        get() = !instance
}

data class AnimatorVisualDefinition(
    val animator: String,
    val animations: Map<String, AnimationFrames>,
    val offsetX: Int = 0,
    val offsetY: Int = 0,
    val surfaceOffsetY: Int = 0,
    val sortLayerOffset: Int = 0,
    override val metadata: Map<String, Any> = emptyMap()
) : VisualDefinition, MetadataHolder {
    override val isShared: Boolean get() = false
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class AnimationFrames(
    val textures: List<String>,
    val speed: Float? = null,
    val offsetX: Int? = null,
    val offsetY: Int? = null
)

enum class Anchor {
    TOP_LEFT, TOP_CENTER, TOP_RIGHT,
    CENTER_LEFT, CENTER, CENTER_RIGHT,
    BOTTOM_LEFT, BOTTOM_CENTER, BOTTOM_RIGHT
}

data class LabelVisualDefinition(
    val label: String,
    val anchor: Anchor = Anchor.CENTER,
    val offsetX: Float = 0f,
    val offsetY: Float = 0f,
    val sortLayerOffset: Int = 0,
    override val metadata: Map<String, Any> = emptyMap()
) : VisualDefinition, MetadataHolder {
    override val isShared: Boolean
        get() = false
}