@file:Suppress("unused")
package world.selene.client.data

import com.badlogic.gdx.utils.Align
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import world.selene.common.data.MetadataHolder
import world.selene.common.data.Registry
import world.selene.common.data.RegistryObject

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes(
    JsonSubTypes.Type(value = SimpleVisualDefinition::class, name = "simple"),
    JsonSubTypes.Type(value = VariantsVisualDefinition::class, name = "variants"),
    JsonSubTypes.Type(value = AnimatedVisualDefinition::class, name = "animated"),
    JsonSubTypes.Type(value = AnimatorVisualDefinition::class, name = "animator"),
    JsonSubTypes.Type(value = TextVisualDefinition::class, name = "text")
)
abstract class VisualDefinition : MetadataHolder, RegistryObject<VisualDefinition> {
    override var id: Int = 0; protected set
    override lateinit var name: String; protected set
    override lateinit var registry: Registry<VisualDefinition>; protected set
    override fun initializeFromRegistry(
        registry: Registry<VisualDefinition>,
        name: String,
        id: Int
    ) {
        this.registry = registry
        this.name = name
        this.id = id
    }
}

data class SimpleVisualDefinition(
    val texture: String,
    val offsetX: Float = 0f,
    val offsetY: Float = 0f,
    val surfaceOffsetY: Float = 0f,
    val sortLayerOffset: Int = 0,
    val flipX: Boolean = false,
    val flipY: Boolean = false,
    override val metadata: Map<String, Any> = emptyMap()
) : VisualDefinition()

data class VariantsVisualDefinition(
    val textures: List<String>,
    val offsetX: Float = 0f,
    val offsetY: Float = 0f,
    val surfaceOffsetY: Float = 0f,
    val sortLayerOffset: Int = 0,
    val flipX: Boolean = false,
    val flipY: Boolean = false,
    override val metadata: Map<String, Any> = emptyMap()
) : VisualDefinition(), MetadataHolder

data class AnimatedVisualDefinition(
    val textures: List<String>,
    val duration: Float = 0.1f,
    val offsetX: Float = 0f,
    val offsetY: Float = 0f,
    val surfaceOffsetY: Float = 0f,
    val sortLayerOffset: Int = 0,
    val flipX: Boolean = false,
    val flipY: Boolean = false,
    val instanced: Boolean = false,
    override val metadata: Map<String, Any> = emptyMap()
) : VisualDefinition(), MetadataHolder

data class AnimatorVisualDefinition(
    val animator: String,
    val animations: Map<String, AnimationFrames>,
    val offsetX: Float = 0f,
    val offsetY: Float = 0f,
    val surfaceOffsetY: Float = 0f,
    val sortLayerOffset: Int = 0,
    override val metadata: Map<String, Any> = emptyMap()
) : VisualDefinition(), MetadataHolder

@JsonIgnoreProperties(ignoreUnknown = true)
data class AnimationFrames(
    val textures: List<String>,
    val speed: Float? = null,
    val offsetX: Float? = null,
    val offsetY: Float? = null,
    val flipX: Boolean = false,
    val flipY: Boolean = false,
)

enum class HorizontalAlign(val align: Int) {
    LEFT(Align.left),
    CENTER(Align.center),
    RIGHT(Align.right)
}

data class TextVisualDefinition(
    val text: String,
    val align: HorizontalAlign = HorizontalAlign.LEFT,
    override val metadata: Map<String, Any> = emptyMap()
) : VisualDefinition(), MetadataHolder