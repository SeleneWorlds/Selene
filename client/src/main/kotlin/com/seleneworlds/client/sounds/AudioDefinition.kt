package com.seleneworlds.client.sounds

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import com.seleneworlds.common.data.MetadataHolder
import com.seleneworlds.common.data.RegistryAdoptedObject
import com.seleneworlds.common.serialization.SerializedMap
import com.seleneworlds.common.serialization.SerializedMapSerializer

@Serializable
sealed interface AudioDefinition

@Serializable
@SerialName("simple")
data class SimpleAudioDefinition(
    val file: String,
    val volume: Float = 1f,
    val pitch: Float = 1f,
    val loop: Boolean = false,
    @Serializable(with = SerializedMapSerializer::class)
    override val metadata: SerializedMap = emptyMap()
) : AudioDefinition, MetadataHolder, RegistryAdoptedObject<AudioDefinition>()

@Serializable
@SerialName("music")
data class MusicAudioDefinition(
    val file: String,
    @Serializable(with = SerializedMapSerializer::class)
    override val metadata: SerializedMap = emptyMap()
) : AudioDefinition, MetadataHolder, RegistryAdoptedObject<AudioDefinition>()
