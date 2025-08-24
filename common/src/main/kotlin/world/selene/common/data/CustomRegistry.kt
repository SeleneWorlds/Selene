package world.selene.common.data

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import world.selene.common.bundles.BundleDatabase
import java.io.File

class CustomRegistry(
    private val objectMapper: ObjectMapper,
    private val definition: RegistryDefinition
) : Registry<JsonNode> {

    private val logger = LoggerFactory.getLogger(CustomRegistry::class.java)
    private val entries: MutableMap<String, JsonNode> = mutableMapOf()
    
    override val name: String = definition.suffix
    override fun get(id: Int): JsonNode? = null
    override fun get(name: String): JsonNode? = entries[name]
    override fun getAll(): Map<String, JsonNode> = entries

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
                        val type = objectMapper.typeFactory.constructParametricType(RegistryFile::class.java, JsonNode::class.java)
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
    }
}
