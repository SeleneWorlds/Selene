package world.selene.common.data

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.BooleanNode
import com.fasterxml.jackson.databind.node.DoubleNode
import com.fasterxml.jackson.databind.node.FloatNode
import com.fasterxml.jackson.databind.node.IntNode
import com.fasterxml.jackson.databind.node.LongNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.databind.node.ShortNode
import com.fasterxml.jackson.databind.node.TextNode
import com.fasterxml.jackson.module.kotlin.treeToValue
import com.google.common.collect.HashBasedTable
import com.google.common.collect.Table
import org.slf4j.LoggerFactory
import party.iroiro.luajava.Lua
import world.selene.common.bundles.BundleDatabase
import world.selene.common.lua.LuaMappedMetatable
import world.selene.common.lua.LuaMetatable
import world.selene.common.lua.LuaMetatableProvider
import world.selene.common.lua.checkString
import java.io.File

class CustomRegistry(
    private val objectMapper: ObjectMapper,
    private val definition: RegistryDefinition
) : Registry<CustomRegistry.CustomRegistryObject> {

    class CustomRegistryObject(val registry: CustomRegistry, val name: String, val element: JsonNode) : LuaMetatableProvider {
        fun getMetadata(key: String): Any? {
            return element["metadata"]?.get(key)?.asAny()
        }

        override fun luaMetatable(lua: Lua): LuaMetatable {
            return luaMeta
        }

        override fun toString(): String {
            return "CustomRegistryObject(${registry.name}, $name, $element)"
        }

        companion object {
            val luaMeta = LuaMappedMetatable(CustomRegistryObject::class) {
                readOnly(CustomRegistryObject::name)
                callable("GetMetadata") {
                    val registryObject = it.checkSelf()
                    val key = it.checkString(2)
                    val value = registryObject.getMetadata(key)
                    if (value != null) {
                        it.push(value, Lua.Conversion.FULL)
                        return@callable 1
                    }
                    0
                }
                callable("GetField") { lua ->
                    val registryObject = lua.checkSelf()
                    val key = lua.checkString(2)
                    when (val value = registryObject.element[key]) {
                        is LongNode -> lua.push(value.asLong())
                        is IntNode, is ShortNode -> lua.push(value.asInt())
                        is FloatNode, is DoubleNode -> lua.push(value.asDouble())
                        is BooleanNode -> lua.push(value.asBoolean())
                        is TextNode -> lua.push(value.asText())
                        is ArrayNode -> lua.push(ObjectMapper().treeToValue(value), Lua.Conversion.FULL)
                        is ObjectNode -> lua.push(ObjectMapper().treeToValue(value), Lua.Conversion.FULL)
                        else -> lua.pushNil()
                    }
                    return@callable 1
                    0
                }
            }
        }
    }

    private val logger = LoggerFactory.getLogger(CustomRegistry::class.java)
    private val entries: MutableMap<String, CustomRegistryObject> = mutableMapOf()
    private val metadataLookupTable: Table<String, Any, MutableList<String>> = HashBasedTable.create()

    override val name: String = definition.suffix
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

}
