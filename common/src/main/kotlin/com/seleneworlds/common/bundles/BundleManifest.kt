package com.seleneworlds.common.bundles

import kotlinx.serialization.Serializable
import com.seleneworlds.common.serialization.SerializedMap
import com.seleneworlds.common.serialization.SerializedMapSerializer

@Serializable
data class BundleManifest(
    val name: String,
    val description: String = name,
    val entrypoints: List<String> = emptyList(),
    val dependencies: List<String> = emptyList(),
    @Serializable(with = SerializedMapSerializer::class)
    val preloads: SerializedMap = emptyMap()
)
