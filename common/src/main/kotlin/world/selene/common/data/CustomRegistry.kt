package world.selene.common.data

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import com.google.common.collect.HashBasedTable
import com.google.common.collect.Table
import org.slf4j.LoggerFactory
import world.selene.common.bundles.BundleDatabase
import world.selene.common.lua.LuaReferenceResolver
import java.io.File
import kotlin.reflect.KClass

class CustomRegistry(
    val objectMapper: ObjectMapper,
    private val definition: RegistryDefinition
) : Registry<CustomRegistryObject>, LuaReferenceResolver<String, CustomRegistryObject> {

    private val logger = LoggerFactory.getLogger(CustomRegistry::class.java)
    private val entries: MutableMap<String, CustomRegistryObject> = mutableMapOf()
    private val metadataLookupTable: Table<String, Any, MutableList<String>> = HashBasedTable.create()

    override val name: String = definition.suffix
    override val clazz: KClass<CustomRegistryObject> = CustomRegistryObject::class
    override fun get(id: Int): CustomRegistryObject? = null
    override fun get(name: String): CustomRegistryObject? = entries[name]
    override fun getAll(): Map<String, CustomRegistryObject> = entries

    override fun findByMetadata(key: String, value: Any): Pair<String, CustomRegistryObject>? {
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
                            entries[name] = CustomRegistryObject(this, name, data)

                            (data.get("metadata") as? ObjectNode)?.forEachEntry { key, node ->
                                val value = node.asAny()
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
                    } catch (e: Exception) {
                        logger.error("Failed to load $file from bundle ${bundle.manifest.name}", e)
                    }
                }
            }
        }
    }

    override fun luaDereference(id: String): CustomRegistryObject? {
        return entries[id]
    }

}
