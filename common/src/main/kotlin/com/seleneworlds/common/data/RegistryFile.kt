package com.seleneworlds.common.data

import kotlinx.serialization.Serializable

@Serializable
data class RegistryFile<T>(val entries: Map<String, T>)
