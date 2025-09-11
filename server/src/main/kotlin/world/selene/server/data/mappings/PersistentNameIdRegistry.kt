package world.selene.server.data.mappings

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.Logger
import world.selene.common.data.mappings.NameIdRegistry
import world.selene.common.data.Registry
import world.selene.server.config.ServerConfig
import java.io.File
import kotlin.collections.iterator

class PersistentNameIdRegistry(
    private val objectMapper: ObjectMapper,
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
            objectMapper.writeValue(mappingsFile, serializedMappings)
            logger.info("Saved id mappings to ${mappingsFile.absolutePath}")
        } catch (e: Exception) {
            logger.error("Failed to save id mappings", e)
        }
    }

    fun load() {
        try {
            if (!mappingsFile.exists()) return
            val serializedMappings =
                objectMapper.readValue(mappingsFile, object : TypeReference<Map<String, Map<String, Int>>>() {})
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
        registry.getAll().keys.forEach { name ->
            getOrAssign(registry.name, name)
        }
        registry.registryPopulated(this)
    }
}