package world.selene.common.data.json

import com.fasterxml.jackson.databind.ObjectMapper
import com.google.common.collect.HashBasedTable
import com.google.common.collect.Table
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import world.selene.common.bundles.Bundle
import world.selene.common.bundles.BundleDatabase
import world.selene.common.data.*
import world.selene.common.data.mappings.NameIdRegistry
import world.selene.common.lua.LuaReferenceResolver
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import kotlin.reflect.KClass

abstract class FileBasedRegistry<TData : Any>(
    val objectMapper: ObjectMapper,
    val platform: String,
    override val name: String,
    private val dataClass: KClass<TData>
) : Registry<TData>, BundleDrivenRegistry, LuaReferenceResolver<Identifier, TData> {
    protected val logger: Logger = LoggerFactory.getLogger("selene")
    protected val entries: MutableMap<Identifier, TData> = mutableMapOf()
    protected val entriesById: MutableMap<Int, TData> = mutableMapOf()
    private val metadataLookupTable: Table<String, Any, MutableList<Identifier>> = HashBasedTable.create()

    override val clazz: KClass<TData> = dataClass
    override fun get(id: Int): TData? = entriesById[id]
    override fun get(identifier: Identifier): TData? = entries[identifier]
    override fun getIdentifier(id: Int): Identifier? {
        return (entriesById[id] as? RegistryOwnedObject<*>)?.identifier
    }

    override fun getAll(): Map<Identifier, TData> = entries

    override fun findByMetadata(key: String, value: Any): Pair<Identifier, TData>? {
        val identifiers = metadataLookupTable[key, value] ?: emptyList()
        val firstIdentifier = identifiers.firstOrNull() ?: return null
        val data = entries[firstIdentifier] ?: return null
        return firstIdentifier to data
    }

    override fun load(bundleDatabase: BundleDatabase) {
        entries.clear()
        entriesById.clear()
        metadataLookupTable.clear()
        for (bundle in bundleDatabase.loadedBundles) {
            val baseDataDir = File(bundle.dir, "$platform/data")
            if (baseDataDir.exists() && baseDataDir.isDirectory) {
                try {
                    val namespaceDirs = baseDataDir.listFiles {
                        it.isDirectory && File(it, name).isDirectory
                    } ?: emptyArray()
                    for (namespaceDir in namespaceDirs) {
                        val namespace = namespaceDir.name
                        val registryDir = File(namespaceDir, name)
                        val registryDirPath = registryDir.toPath()
                        Files.walk(registryDirPath).use { stream ->
                            stream.filter { path -> Files.isRegularFile(path) && path.toString().endsWith(".json") }
                                .forEach { path ->
                                    try {
                                        val relativePath = registryDirPath.relativize(path)
                                        val entryName =
                                            relativePath.toString().removeSuffix(".json")
                                                .replace(File.separatorChar, '/')
                                        val identifier = Identifier(namespace, entryName)

                                        val data = loadEntryFromFile(path, identifier)
                                        if (data != null) {
                                            entries[identifier] = data
                                            (data as? MetadataHolder)?.let { addToMetadataLookup(identifier, it) }
                                        }
                                    } catch (e: Exception) {
                                        logger.error("Failed to load $path from bundle ${bundle.manifest.name}", e)
                                    }
                                }
                        }
                    }
                } catch (e: Exception) {
                    logger.error("Failed to walk directory tree $baseDataDir for bundle ${bundle.manifest.name}", e)
                }
            }
        }
    }

    protected open fun loadEntryFromFile(path: Path, identifier: Identifier): TData? {
        val data = objectMapper.readValue(path.toFile(), dataClass.java)

        @Suppress("UNCHECKED_CAST")
        return data.also {
            (it as? RegistryAdoptedObject<TData>)?.registry = this@FileBasedRegistry
            (it as? RegistryAdoptedObject<*>)?.identifier = identifier
        }
    }

    override fun registryPopulated(mappings: NameIdRegistry, throwOnMissingId: Boolean) {
        for ((identifier, data) in entries) {
            val id = mappings.getId(this.name, identifier.toString())
                ?: if (throwOnMissingId) throw RuntimeException("Missing id mapping for $identifier in ${this.name}") else -1
            (data as? IdMappedObject)?.id = id
            if (id != -1) {
                entriesById[id] = data
            }
        }
    }

    private fun filePathToIdentifier(filePath: String): Identifier? {
        val matchResult = registryJsonPattern.matchEntire(filePath.replace('\\', '/'))
        if (matchResult != null) {
            val platform = matchResult.groupValues[1]
            if (platform != this.platform) {
                return null
            }
            val namespace = matchResult.groupValues[2]
            val registryName = matchResult.groupValues[3]
            if (registryName != this.name) {
                return null
            }
            val path = matchResult.groupValues[4]
            return Identifier(namespace, path)
        }

        return null
    }

    override fun bundleFileUpdated(
        bundleDatabase: BundleDatabase,
        bundle: Bundle,
        path: String
    ) {
        val identifier = filePathToIdentifier(path) ?: return
        val data = loadEntryFromFile(bundle.dir.toPath().resolve(path), identifier)
        if (data != null) {
            val oldEntry = entries.put(identifier, data)
            if (oldEntry is RegistryObject<*> && data is RegistryObject<*>) {
                val id = oldEntry.id
                if (id != -1) {
                    entriesById[id] = data
                    (data as IdMappedObject).id = id
                }
            }

            (oldEntry as? MetadataHolder)?.let { removeFromMetadataLookup(identifier, it) }
            (data as? MetadataHolder)?.let { addToMetadataLookup(identifier, it) }

            subscriptions[identifier]?.forEach { handler ->
                handler(data)
            }

            logger.info("Updated registry entry $identifier in ${this.name} due to file update: $path")
        } else {
            logger.warn("Failed to load updated entry $identifier from $path")
        }
    }

    override fun bundleFileRemoved(
        bundleDatabase: BundleDatabase,
        bundle: Bundle,
        path: String
    ) {
        val identifier = filePathToIdentifier(path) ?: return
        val removedEntry = entries.remove(identifier)
        (removedEntry as? RegistryObject<*>)?.let { entriesById.remove(it.id) }
        (removedEntry as? MetadataHolder)?.let { removeFromMetadataLookup(identifier, it) }
    }

    private fun addToMetadataLookup(identifier: Identifier, metadataHolder: MetadataHolder) {
        metadataHolder.metadata.forEach { (key, value) ->
            val list = metadataLookupTable.get(key, value)
            if (list != null) {
                list.add(identifier)
            } else {
                metadataLookupTable.put(key, value, mutableListOf(identifier))
            }
        }
    }

    private fun removeFromMetadataLookup(identifier: Identifier, metadataHolder: MetadataHolder) {
        metadataHolder.metadata.forEach { (key, value) ->
            val identifierList = metadataLookupTable.get(key, value)
            if (identifierList != null) {
                identifierList.remove(identifier)
                if (identifierList.isEmpty()) {
                    metadataLookupTable.remove(key, value)
                }
            }
        }
    }

    private val subscriptions: MutableMap<Identifier, MutableList<(TData) -> Unit>> = mutableMapOf()

    override fun subscribe(
        reference: RegistryReference<TData>,
        handler: (TData) -> Unit
    ) {
        val resolved = reference.get()
        if (resolved != null) {
            handler(resolved)
        }

        subscriptions.getOrPut(reference.identifier) { mutableListOf() }.add(handler)
    }

    override fun luaDereference(id: Identifier): TData? {
        return entries[id]
    }

    companion object {
        private val registryJsonPattern = "(common|server|client)/data/([\\w-]+)/([\\w-]+)/(.+)\\.json".toRegex()
    }
}
