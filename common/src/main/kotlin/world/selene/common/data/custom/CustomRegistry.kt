package world.selene.common.data.custom

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import com.google.common.collect.HashBasedTable
import com.google.common.collect.Table
import org.slf4j.LoggerFactory
import world.selene.common.bundles.BundleDatabase
import world.selene.common.data.Registry
import world.selene.common.util.asAny
import world.selene.common.lua.LuaReferenceResolver
import java.io.File
import java.nio.file.Files
import kotlin.reflect.KClass

class CustomRegistry(
    val objectMapper: ObjectMapper,
    private val definition: CustomRegistryDefinition
) : Registry<CustomRegistryObject>, LuaReferenceResolver<String, CustomRegistryObject> {

    private val logger = LoggerFactory.getLogger(CustomRegistry::class.java)
    private val entries: MutableMap<String, CustomRegistryObject> = mutableMapOf()
    private val metadataLookupTable: Table<String, Any, MutableList<String>> = HashBasedTable.create()

    override val name: String = definition.name
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
            val baseDataDir = File(bundle.dir, "${definition.platform}/data")
            if (baseDataDir.exists() && baseDataDir.isDirectory) {
                try {
                    val namespaceDirs = baseDataDir.listFiles {
                        it.isDirectory && File(it, name).isDirectory
                    } ?: emptyArray()
                    for (namespaceDir in namespaceDirs) {
                        val namespace = namespaceDir.name
                        val registryDir = File(namespaceDir, name)
                        val registryDirPath = registryDir.toPath()
                        Files.walk(registryDirPath)
                            .filter { path -> Files.isRegularFile(path) && path.toString().endsWith(".json") }
                            .forEach { path ->
                                try {
                                    val relativePath = registryDirPath.relativize(path)
                                    val entryName = relativePath.toString().removeSuffix(".json").replace(File.separatorChar, '/')
                                    val fullName = "$namespace:$entryName"
                                    
                                    val data = objectMapper.readTree(path.toFile())
                                    val customObject = CustomRegistryObject(this, fullName, data)

                                    entries[fullName] = customObject

                                    // Process metadata for lookup
                                    (data.get("metadata") as? ObjectNode)?.forEachEntry { key, node ->
                                        val value = node.asAny()
                                        if (value != null) {
                                            val list = metadataLookupTable.get(key, value)
                                            if (list != null) {
                                                list.add(fullName)
                                            } else {
                                                metadataLookupTable.put(key, value, mutableListOf(fullName))
                                            }
                                        }
                                    }
                                } catch (e: Exception) {
                                    logger.error("Failed to load $path from bundle ${bundle.manifest.name}", e)
                                }
                            }
                    }
                } catch (e: Exception) {
                    logger.error("Failed to walk directory tree $baseDataDir for bundle ${bundle.manifest.name}", e)
                }
            }
        }
    }

    override fun luaDereference(id: String): CustomRegistryObject? {
        return entries[id]
    }

}
