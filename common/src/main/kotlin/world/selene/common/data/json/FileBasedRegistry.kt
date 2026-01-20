package world.selene.common.data.json

import com.fasterxml.jackson.databind.ObjectMapper
import com.google.common.collect.HashBasedTable
import com.google.common.collect.Table
import org.slf4j.LoggerFactory
import world.selene.common.bundles.BundleDatabase
import world.selene.common.data.MetadataHolder
import world.selene.common.data.Registry
import world.selene.common.data.RegistryObject
import world.selene.common.data.mappings.NameIdRegistry
import java.io.File
import java.nio.file.Files
import kotlin.reflect.KClass

abstract class FileBasedRegistry<TData : Any>(
    private val objectMapper: ObjectMapper,
    val platform: String,
    override val name: String,
    private val dataClass: KClass<TData>
) : Registry<TData> {
    private val logger = LoggerFactory.getLogger("selene")
    protected val entries: MutableMap<String, TData> = mutableMapOf()
    protected val entriesById: MutableMap<Int, TData> = mutableMapOf()
    private val metadataLookupTable: Table<String, Any, MutableList<String>> = HashBasedTable.create()

    override val clazz: KClass<TData> = dataClass
    override fun get(id: Int): TData? = entriesById[id]
    override fun get(name: String): TData? = entries[name]
    override fun getAll(): Map<String, TData> = entries

    override fun findByMetadata(key: String, value: Any): Pair<String, TData>? {
        val entryNames = metadataLookupTable[key, value] ?: emptyList()
        val firstEntryName = entryNames.firstOrNull() ?: return null
        val data = entries[firstEntryName] ?: return null
        return firstEntryName to data
    }

    fun load(bundleDatabase: BundleDatabase) {
        entries.clear()
        entriesById.clear()
        metadataLookupTable.clear() // Clear cache when reloading entries
        for (bundle in bundleDatabase.loadedBundles) {
            val dataDir = File(bundle.dir, "$platform/data/$name")
            if (dataDir.exists() && dataDir.isDirectory) {
                try {
                    val dataDirPath = dataDir.toPath()
                    Files.walk(dataDirPath)
                        .filter { path -> Files.isRegularFile(path) && path.toString().endsWith(".json") }
                        .forEach { path ->
                            try {
                                val relativePath = dataDirPath.relativize(path)
                                val entryName = relativePath.toString().removeSuffix(".json").replace(File.separatorChar, '/')
                                val data = objectMapper.readValue(path.toFile(), dataClass.java)

                                @Suppress("UNCHECKED_CAST")
                                entries[entryName] = data.apply {
                                    if (data is RegistryObject<*>) {
                                        // We do an early init without an id so that the item is immediately aware of its parent registry.
                                        (data as RegistryObject<TData>).initializeFromRegistry(
                                            this@FileBasedRegistry,
                                            entryName,
                                            -1
                                        )
                                    }
                                }
                            } catch (e: Exception) {
                                logger.error("Failed to load $path from bundle ${bundle.manifest.name}", e)
                            }
                        }
                } catch (e: Exception) {
                    logger.error("Failed to walk directory tree $dataDir for bundle ${bundle.manifest.name}", e)
                }
            }
        }
    }

    override fun registryPopulated(mappings: NameIdRegistry, throwOnMissingId: Boolean) {
        for ((name, data) in entries) {
            val id = mappings.getId(this.name, name)
                ?: if (throwOnMissingId) throw RuntimeException("Missing id mapping for $name in ${this.name}") else -1
            @Suppress("UNCHECKED_CAST")
            ((data as? RegistryObject<TData>)?.initializeFromRegistry(this, name, id))
            if (id != -1) {
                entriesById[id] = data
            }

            if (data is MetadataHolder) {
                data.metadata.forEach { (key, value) ->
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
