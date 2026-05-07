package com.seleneworlds.server.saves

import kotlinx.serialization.json.Json
import com.seleneworlds.common.observable.ObservableMap
import com.seleneworlds.common.serialization.SerializedMapSerializer
import com.seleneworlds.common.serialization.decodeFromFile
import com.seleneworlds.server.maps.tree.MapTree
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.JsonElement
import java.io.File

class SaveManager(
    private val mapTreeFormat: MapTreeFormat,
    private val json: Json
) {

    fun save(file: File, savable: Any?) {
        file.parentFile.mkdirs()
        when (savable) {
            is MapTree -> mapTreeFormat.saveFullyInline(file, savable)
            is ObservableMap -> file.writeText(json.encodeToString(SerializedMapSerializer, savable.map))
            is Map<*, *> -> {
                @Suppress("UNCHECKED_CAST")
                file.writeText(json.encodeToString(SerializedMapSerializer, savable as Map<String, Any?>))
            }
        }
    }

    fun load(file: File): Any? {
        return when (file.extension) {
            "selenemap" -> mapTreeFormat.load(file)
            "json" -> ObservableMap(json.decodeFromFile(SerializedMapSerializer, file).toMutableMap())
            else -> null
        }
    }

    fun loadKeyValueMap(file: File): Map<String, Any?> {
        return json.decodeFromFile(SerializedMapSerializer, file)
    }

}
