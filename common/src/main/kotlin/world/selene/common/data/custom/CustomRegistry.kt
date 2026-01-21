package world.selene.common.data.custom

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import com.google.common.collect.HashBasedTable
import com.google.common.collect.Table
import org.slf4j.LoggerFactory
import world.selene.common.bundles.Bundle
import world.selene.common.bundles.BundleDatabase
import world.selene.common.data.BundleDrivenRegistry
import world.selene.common.data.Registry
import world.selene.common.data.Identifier
import world.selene.common.util.asAny
import world.selene.common.lua.LuaReferenceResolver
import java.io.File
import java.nio.file.Files
import kotlin.reflect.KClass

class CustomRegistry(
    val objectMapper: ObjectMapper,
    private val definition: CustomRegistryDefinition
) : Registry<CustomRegistryObject>, BundleDrivenRegistry, LuaReferenceResolver<Identifier, CustomRegistryObject> {

    private val logger = LoggerFactory.getLogger(CustomRegistry::class.java)
    private val entries: MutableMap<Identifier, CustomRegistryObject> = mutableMapOf()
    private val metadataLookupTable: Table<String, Any, MutableList<Identifier>> = HashBasedTable.create()

    override val name: String = definition.name
    override val clazz: KClass<CustomRegistryObject> = CustomRegistryObject::class
    override fun get(id: Int): CustomRegistryObject? = null
    override fun get(identifier: Identifier): CustomRegistryObject? = entries[identifier]
    override fun getAll(): Map<Identifier, CustomRegistryObject> = entries.mapKeys { it.key }

    override fun findByMetadata(key: String, value: Any): Pair<Identifier, CustomRegistryObject>? {
        val identifiers = metadataLookupTable[key, value] ?: emptyList()
        val firstIdentifier = identifiers.firstOrNull() ?: return null
        val data = entries[firstIdentifier] ?: return null
        return firstIdentifier to data
    }

    override fun load(bundleDatabase: BundleDatabase) {
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
                                    val entryPath = relativePath.toString().removeSuffix(".json").replace(File.separatorChar, '/')
                                    val identifier = Identifier(namespace, entryPath)
                                    
                                    val data = objectMapper.readTree(path.toFile())
                                    val customObject = CustomRegistryObject(this, identifier, data)

                                    entries[identifier] = customObject

                                    // Process metadata for lookup
                                    (data.get("metadata") as? ObjectNode)?.forEachEntry { key, node ->
                                        val value = node.asAny()
                                        if (value != null) {
                                            val list = metadataLookupTable.get(key, value)
                                            if (list != null) {
                                                list.add(identifier)
                                            } else {
                                                metadataLookupTable.put(key, value, mutableListOf(identifier))
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

    override fun luaDereference(id: Identifier): CustomRegistryObject? {
        return entries[id]
    }

    override fun bundleFileUpdated(
        bundleDatabase: BundleDatabase,
        bundle: Bundle,
        path: String
    ): Boolean {
        TODO("Not yet implemented")
    }

    override fun bundleFileRemoved(
        bundleDatabase: BundleDatabase,
        bundle: Bundle,
        path: String
    ): Boolean {
        TODO("Not yet implemented")
    }
}
