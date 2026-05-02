package com.seleneworlds.common.data.custom

import kotlinx.serialization.Serializable

@Serializable
data class CustomRegistryDefinition(
    val name: String,
    val platform: String
)
