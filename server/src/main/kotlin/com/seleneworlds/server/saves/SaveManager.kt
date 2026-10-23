package com.seleneworlds.server.saves

import kotlinx.serialization.json.Json
import com.seleneworlds.common.serialization.SerializedMapSerializer
import com.seleneworlds.common.serialization.decodeFromFile
import com.seleneworlds.server.maps.tree.MapTree
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.JsonElement
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.nio.file.AtomicMoveNotSupportedException

class SaveManager(
    private val mapTreeFormat: MapTreeFormat,
    private val json: Json
) {

    fun save(file: File, savable: Any?) {
        file.parentFile.mkdirs()
        when (savable) {
            is MapTree -> {
                // Never truncate the last good map save if serialization fails.
                val temporary = Files.createTempFile(file.absoluteFile.parentFile.toPath(), ".map-", ".tmp")
                try {
                    mapTreeFormat.saveFullyInline(temporary.toFile(), savable)
                    try {
                        Files.move(temporary, file.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
                    } catch (_: AtomicMoveNotSupportedException) {
                        Files.move(temporary, file.toPath(), StandardCopyOption.REPLACE_EXISTING)
                    }
                } finally {
                    Files.deleteIfExists(temporary)
                }
            }
            is Map<*, *> -> {
                @Suppress("UNCHECKED_CAST")
                file.writeText(json.encodeToString(SerializedMapSerializer, savable as Map<String, Any?>))
            }
        }
    }

    fun load(file: File): Any? {
        return when (file.extension) {
            "selenemap" -> mapTreeFormat.load(file)
            "json" -> json.decodeFromFile(SerializedMapSerializer, file).toMutableMap()
            else -> null
        }
    }

    fun loadKeyValueMap(file: File): Map<String, Any?> {
        return json.decodeFromFile(SerializedMapSerializer, file)
    }

}
