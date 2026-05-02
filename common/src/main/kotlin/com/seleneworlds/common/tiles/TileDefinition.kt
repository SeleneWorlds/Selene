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
    @Serializable(with = SerializedMapSerializer::class)
    override val metadata: SerializedMap = emptyMap(),
    override val tags: Set<String> = emptySet()
) : MetadataHolder, TagHolder, RegistryAdoptedObject<TileDefinition>()
