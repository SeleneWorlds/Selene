package com.seleneworlds.server.bundles

import com.seleneworlds.common.bundles.Bundle
import com.seleneworlds.common.bundles.BundleDatabase
import com.seleneworlds.common.data.Identifier
import com.seleneworlds.common.serialization.decodeFromFile
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap
import kotlin.io.path.extension
import kotlin.io.path.isRegularFile

class ClientRegistrySnapshots(
    private val bundleDatabase: BundleDatabase,
    private val clientBundleCache: ClientBundleCache,
    private val json: Json
) {
    private val snapshotsByCacheKey = ConcurrentHashMap<String, ClientRegistrySnapshotsData>()

    fun getIndex(): ClientRegistryIndexResponse {
        val snapshots = getSnapshots()
        return ClientRegistryIndexResponse(
            hash = snapshots.hash,
            registries = snapshots.registries.mapValues { (registry, snapshot) ->
                ClientRegistryIndexEntry(
                    hash = snapshot.hash,
                    url = "/client/registries/$registry"
                )
            }.toSortedMap()
        )
    }

    fun getRegistry(registry: Identifier): ClientRegistrySnapshotResponse? {
        return getSnapshots().registries[registry.toString()]
    }

    private fun getSnapshots(): ClientRegistrySnapshotsData {
        val enabledClientBundles = bundleDatabase.enabledBundles
            .filter { clientBundleCache.hasClientSide(it.dir) }

        val cacheKey = enabledClientBundles.joinToString("|") { bundle ->
            "${bundle.manifest.name}:${clientBundleCache.getHash(bundle.dir).orEmpty()}"
        }

        return snapshotsByCacheKey.getOrPut(cacheKey) {
            buildSnapshots(enabledClientBundles)
        }
    }

    private fun buildSnapshots(bundles: List<Bundle>): ClientRegistrySnapshotsData {
        val customRegistryIdentifiers = collectCustomRegistryIdentifiers(bundles)
        val entriesByRegistry = mutableMapOf<Identifier, MutableMap<Identifier, JsonElement>>()

        for (bundle in bundles) {
            for (platform in CLIENT_PLATFORMS) {
                val dataDir = bundle.dir.resolve("$platform/data")
                if (!dataDir.isDirectory) {
                    continue
                }

                loadRegistryFiles(dataDir.toPath(), platform, customRegistryIdentifiers, entriesByRegistry)
            }
        }

        val registries = entriesByRegistry
            .mapKeys { it.key.toString() }
            .mapValues { (registry, entries) ->
                val response = ClientRegistrySnapshotResponse(
                    registry = registry,
                    hash = hashEntries(entries),
                    entries = entries
                        .toSortedMap()
                        .mapKeys { it.key.toString() }
                )
                response
            }
            .toSortedMap()

        return ClientRegistrySnapshotsData(
            hash = hashText(registries.values.joinToString("|") { "${it.registry}:${it.hash}" }),
            registries = registries
        )
    }

    private fun collectCustomRegistryIdentifiers(bundles: List<Bundle>): Map<CustomRegistryKey, Identifier> {
        val customRegistryIdentifiers = mutableMapOf<CustomRegistryKey, Identifier>()

        for (bundle in bundles) {
            val registriesDir = bundle.dir.resolve("common/data")
            if (!registriesDir.isDirectory) {
                continue
            }

            Files.walk(registriesDir.toPath()).use { stream ->
                stream
                    .filter { it.isRegularFile() && it.extension == "json" }
                    .filter { isRegistryDefinitionPath(registriesDir.toPath(), it) }
                    .sorted()
                    .forEach { path ->
                        loadCustomRegistryDefinition(registriesDir.toPath(), path)?.let { definition ->
                            customRegistryIdentifiers[CustomRegistryKey(definition.platform, definition.name)] =
                                definition.identifier
                        }
                    }
            }
        }

        return customRegistryIdentifiers
    }

    private fun loadCustomRegistryDefinition(dataDir: Path, path: Path): CustomRegistryDefinitionEntry? {
        val relativePath = dataDir.relativize(path)
        val parts = relativePath.map { it.toString() }.toList()
        if (parts.size < 3 || parts[1] != "registries") {
            return null
        }

        val namespace = parts[0]
        val registryPath = parts.drop(2).joinToString("/").removeSuffix(".json")
        val element = json.decodeFromFile(JsonElement.serializer(), path).jsonObject
        val name = element["name"]?.jsonPrimitive?.content ?: return null
        val platform = element["platform"]?.jsonPrimitive?.content ?: return null
        return CustomRegistryDefinitionEntry(
            identifier = Identifier(namespace, registryPath),
            name = name,
            platform = platform
        )
    }

    private fun loadRegistryFiles(
        dataDir: Path,
        platform: String,
        customRegistryIdentifiers: Map<CustomRegistryKey, Identifier>,
        entriesByRegistry: MutableMap<Identifier, MutableMap<Identifier, JsonElement>>
    ) {
        Files.walk(dataDir).use { stream ->
            stream
                .filter { it.isRegularFile() && it.extension == "json" }
                .sorted()
                .forEach { path ->
                    loadRegistryFile(dataDir, path, platform, customRegistryIdentifiers, entriesByRegistry)
                }
        }
    }

    private fun loadRegistryFile(
        dataDir: Path,
        path: Path,
        platform: String,
        customRegistryIdentifiers: Map<CustomRegistryKey, Identifier>,
        entriesByRegistry: MutableMap<Identifier, MutableMap<Identifier, JsonElement>>
    ) {
        val relativePath = dataDir.relativize(path)
        val parts = relativePath.map { it.toString() }.toList()
        if (parts.size < 2) {
            return
        }

        val namespace = parts[0]
        val registryName = parts[1].removeSuffix(".json")
        val registryIdentifier = customRegistryIdentifiers[CustomRegistryKey(platform, registryName)]
            ?: BUILTIN_REGISTRY_IDENTIFIERS[registryName]
            ?: Identifier.withDefaultNamespace(registryName)
        val registryEntries = entriesByRegistry.getOrPut(registryIdentifier) { mutableMapOf() }

        if (parts.size == 2) {
            val registryFile = json.decodeFromFile(JsonObject.serializer(), path)
            val entries = registryFile["entries"]?.jsonObject ?: return
            for ((entryName, entry) in entries) {
                registryEntries[Identifier(namespace, entryName)] = entry
            }
            return
        }

        val entryPath = parts.drop(2).joinToString("/").removeSuffix(".json")
        registryEntries[Identifier(namespace, entryPath)] = json.decodeFromFile(JsonElement.serializer(), path)
    }

    private fun isRegistryDefinitionPath(dataDir: Path, path: Path): Boolean {
        val parts = dataDir.relativize(path).map { it.toString() }.toList()
        return parts.size >= 3 && parts[1] == "registries"
    }

    private fun hashEntries(entries: Map<Identifier, JsonElement>): String {
        val jsonObject = JsonObject(entries.toSortedMap().mapKeys { it.key.toString() })
        val text = json.encodeToString(
            MapSerializer(String.serializer(), JsonElement.serializer()),
            jsonObject
        )
        return hashText(text)
    }

    private fun hashText(text: String): String {
        val hasher = MessageDigest.getInstance("SHA-256")
        return hasher.digest(text.toByteArray()).joinToString("") { "%02x".format(it) }
    }

    private data class CustomRegistryKey(val platform: String, val name: String)

    private data class CustomRegistryDefinitionEntry(
        val identifier: Identifier,
        val name: String,
        val platform: String
    )

    private data class ClientRegistrySnapshotsData(
        val hash: String,
        val registries: Map<String, ClientRegistrySnapshotResponse>
    )

    private companion object {
        val CLIENT_PLATFORMS = listOf("common", "client")
        val BUILTIN_REGISTRY_IDENTIFIERS = setOf(
            "audio",
            "components",
            "entities",
            "grids",
            "registries",
            "render_grids",
            "sounds",
            "tiles",
            "transitions",
            "visuals"
        ).associateWith { Identifier.withDefaultNamespace(it) }
    }
}

@Serializable
data class ClientRegistryIndexResponse(
    val hash: String,
    val registries: Map<String, ClientRegistryIndexEntry>
)

@Serializable
data class ClientRegistryIndexEntry(
    val hash: String,
    val url: String
)

@Serializable
data class ClientRegistrySnapshotResponse(
    val registry: String,
    val hash: String,
    val entries: Map<String, JsonElement>
)
