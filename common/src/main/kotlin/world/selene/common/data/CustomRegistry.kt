package world.selene.common.data

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.BooleanNode
import com.fasterxml.jackson.databind.node.DoubleNode
import com.fasterxml.jackson.databind.node.FloatNode
import com.fasterxml.jackson.databind.node.IntNode
import com.fasterxml.jackson.databind.node.LongNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.databind.node.ShortNode
import com.fasterxml.jackson.databind.node.TextNode
import com.google.common.collect.HashBasedTable
import com.google.common.collect.Table
import org.slf4j.LoggerFactory
import world.selene.common.bundles.BundleDatabase
import java.io.File

class CustomRegistry(
    private val objectMapper: ObjectMapper,
    private val definition: RegistryDefinition
) : Registry<JsonNode> {

    private val logger = LoggerFactory.getLogger(CustomRegistry::class.java)
    private val entries: MutableMap<String, JsonNode> = mutableMapOf()
    private val metadataLookupTable: Table<String, Any, MutableList<String>> = HashBasedTable.create()

    override val name: String = definition.suffix
    override fun get(id: Int): JsonNode? = null
    override fun get(name: String): JsonNode? = entries[name]
    override fun getAll(): Map<String, JsonNode> = entries

    override fun findByMetadata(key: String, value: Any): Pair<String, JsonNode>? {
        val entryNames = metadataLookupTable[key, value] ?: emptyList()
        val firstEntryName = entryNames.firstOrNull() ?: return null
        val data = entries[firstEntryName] ?: return null
        return firstEntryName to data
    }

    fun load(bundleDatabase: BundleDatabase) {
        entries.clear()
        for (bundle in bundleDatabase.loadedBundles) {
            val dataDir = File(bundle.dir, "${definition.platform}/data")
            val files = dataDir.listFiles { _, file ->
                file == "${definition.suffix}.json" || file.endsWith(".${definition.suffix}.json")
            }
            if (files != null) {
                for (file in files) {
                    try {
                        val type = objectMapper.typeFactory.constructParametricType(
                            RegistryFile::class.java,
                            JsonNode::class.java
                        )
                        val parsed = objectMapper.readValue<RegistryFile<JsonNode>>(file, type)
                        for ((name, data) in parsed.entries) {
                            entries[name] = data
                        }
                    } catch (e: Exception) {
                        logger.error("Failed to load $file from bundle ${bundle.manifest.name}", e)
                    }
                }
            }
        }
    }

    override fun registryPopulated(mappings: NameIdRegistry) {
        for ((name, data) in entries) {
            (data.get("metadata") as? ObjectNode)?.forEachEntry { key, node ->
                val value = when (node) {
                    is LongNode -> node.asLong()
                    is IntNode, is ShortNode -> node.asInt()
                    is FloatNode, is DoubleNode -> node.asDouble()
                    is BooleanNode -> node.asBoolean()
                    is TextNode -> node.asText()
                    else -> null
                }
                if (value != null) {
                    val list = metadataLookupTable.get(key, value)
                    if (list != null) {
                        list.add(name)
                    } else {
                        metadataLookupTable.put(key, value, mutableListOf(name))
                    }
                }
            }
        }
    }
}
