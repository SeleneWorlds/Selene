package com.seleneworlds.common.data.custom

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class CustomRegistryDefinition(
    val name: String,
    val platform: String,
    val icon: String? = null,
    val schema: JsonObject? = null
)
