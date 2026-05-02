package com.seleneworlds.common.tiles

import com.seleneworlds.common.data.Identifier
import com.seleneworlds.common.data.MetadataHolder
import com.seleneworlds.common.data.RegistryAdoptedObject
import com.seleneworlds.common.data.TagHolder

data class TileDefinition(
    val visual: Identifier,
    val impassable: Boolean = false,
    val passableAbove: Boolean = false,
    override val metadata: Map<String, Any> = emptyMap(),
    override val tags: Set<String> = emptySet()
) : MetadataHolder, TagHolder, RegistryAdoptedObject<TileDefinition>()