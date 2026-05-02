package com.seleneworlds.common.sounds

import com.seleneworlds.common.data.MetadataHolder
import com.seleneworlds.common.data.RegistryAdoptedObject
import com.seleneworlds.common.data.TagHolder

data class SoundDefinition(
    val audio: String,
    override val metadata: Map<String, Any> = emptyMap(),
    override val tags: Set<String> = emptySet()
) : MetadataHolder, TagHolder, RegistryAdoptedObject<SoundDefinition>()