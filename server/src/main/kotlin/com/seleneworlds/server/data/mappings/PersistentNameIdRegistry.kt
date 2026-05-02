package com.seleneworlds.server.data.mappings

import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import org.slf4j.Logger
import com.seleneworlds.common.data.mappings.NameIdRegistry
import com.seleneworlds.common.data.Registry
import com.seleneworlds.common.serialization.decodeFromFile
import com.seleneworlds.server.config.ServerConfig
import java.io.File
import kotlin.collections.iterator

class PersistentNameIdRegistry(
    private val json: Json,
    private val config: ServerConfig,
    private val logger: Logger
) : NameIdRegistry() {

    private val mappingsFile: File
        get() = File(config.savePath, "id_mappings.json")
    private var isDirty = false
    private var nextId = 1

    fun save() {
        if (!isDirty) return

        if (!mappingsFile.parentFile.exists() && !mappingsFile.parentFile.mkdirs()) {
            logger.error("Failed to create save directory ${mappingsFile.parentFile.absolutePath}")
            return
        }

        try {
            val serializedMappings = mutableMapOf<String, MutableMap<String, Int>>()
            for (cell in mappings.cellSet()) {
                val scopeMap = serializedMappings.getOrPut(cell.rowKey) { mutableMapOf() }
                scopeMap[cell.columnKey] = cell.value
            }
            mappingsFile.writeText(
                json.encodeToString(
                    MapSerializer(String.serializer(), MapSerializer(String.serializer(), Int.serializer())),
                    serializedMappings
                )
            )
            logger.info("Saved id mappings to ${mappingsFile.absolutePath}")
        } catch (e: Exception) {
            logger.error("Failed to save id mappings", e)
        }
    }

    fun load() {
        try {
            if (!mappingsFile.exists()) return
            val serializedMappings = json.decodeFromFile(
                MapSerializer(String.serializer(), MapSerializer(String.serializer(), Int.serializer())),
                mappingsFile
            )
            clearAll()
            for ((scope, nameToId) in serializedMappings) {
                for ((name, id) in nameToId) {
                    mappings.put(scope, name, id)
                    reverseMappings.put(scope, id, name)
                    nextId = maxOf(nextId, id + 1)
                }
            }
            logger.info("Loaded id mappings from ${mappingsFile.absolutePath}")
        } catch (e: Exception) {
            logger.error("Failed to load id mappings", e)
        }
    }

    fun getOrAssign(scope: String, name: String): Int {
        val id = getId(scope, name)
        if (id != null) return id
        val newId = nextId
        nextId++
        isDirty = true
        mappings.put(scope, name, newId)
        reverseMappings.put(scope, newId, name)
        return newId
    }

    fun populate(registry: Registry<*>) {
        registry.getAll().keys.forEach { identifier ->
            getOrAssign(registry.name, identifier.toString())
        }
        registry.registryPopulated(this)
    }
}
