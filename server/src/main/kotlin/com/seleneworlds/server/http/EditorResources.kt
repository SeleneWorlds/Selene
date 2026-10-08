package com.seleneworlds.server.http

import com.seleneworlds.common.bundles.BundleDatabase
import com.seleneworlds.common.bundles.ResourcesApi
import com.seleneworlds.common.data.Identifier
import com.seleneworlds.common.data.MetadataHolder
import com.seleneworlds.common.data.RegistriesApi
import com.seleneworlds.common.data.RegistryProvider
import com.seleneworlds.common.data.custom.CustomRegistryObject
import com.seleneworlds.common.data.json.FileBasedRegistry
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import com.seleneworlds.common.serialization.seleneJson
import com.seleneworlds.common.serialization.SerializedMapSerializer
import com.seleneworlds.common.threading.MainThreadDispatcher
import com.seleneworlds.server.permissions.PermissionsApi
import com.seleneworlds.server.players.PlayerManager
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.receiveText
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.slf4j.Logger

/** Resource editing and search. Callers authorize access and execute on the main thread. */
class EditorResources(
    private val resources: ResourcesApi,
    private val registryProvider: RegistryProvider,
    private val json: Json,
    private val bundles: BundleDatabase
) {
    private val resourceJson = Json(json) { prettyPrint = true }
    private val registries = RegistriesApi(registryProvider)
    private val builtinSchemas: JsonObject by lazy {
        val contents = requireNotNull(javaClass.getResourceAsStream("/schemas/registries.json")) {
            "Missing built-in registry schemas"
        }.bufferedReader(Charsets.UTF_8).use { it.readText() }
        json.parseToJsonElement(contents).jsonObject
    }

    fun searchRegistry(request: RegistrySearchRequest): RegistrySearchResult {
        val options = registries.findAll(request.registry).mapNotNull { (identifier, entry) ->
            val value = identifier.toString()
            val metadata = (entry as? MetadataHolder)?.metadata.orEmpty()
            val fields = (entry as? CustomRegistryObject)?.element as? JsonObject
            val label = (metadata["name"] ?: fields.string("name") ?: value).toString()
            val matches = if (request.lookup) value.equals(request.query, ignoreCase = true)
                else label.contains(request.query, ignoreCase = true) || value.contains(request.query, ignoreCase = true)
            if (!matches) return@mapNotNull null

            val visual = metadata["visual"] as? String ?: fields.string("visual")
                ?: value.takeIf { request.registry == "entities" || request.registry.endsWith(":entities") }
            RegistryOption(value, label, visual)
        }.sortedBy { it.label }.take(50)
        return RegistrySearchResult(request.registry, request.query, request.lookup, options)
    }

    fun searchScripts(request: ScriptSearchRequest): ScriptSearchResult {
        val names = sortedSetOf<String>()
        for (bundle in bundles.enabledBundles) {
            val bundleName = bundle.manifest.name
            for ((name, preload) in bundle.manifest.preloads) {
                val file = preload as? String ?: (preload as? Map<*, *>)?.get("file") as? String ?: continue
                if (!file.startsWith("client/") && resources.fileExists("$bundleName/$file")) names.add(name)
            }
            for (path in resources.listFiles(bundleName, "*.lua")) {
                val relative = path.removePrefix("$bundleName/")
                when {
                    relative == "init.lua" -> names.add(bundleName)
                    relative.startsWith("common/lua/") || relative.startsWith("server/lua/") ->
                        names.add("$bundleName.${relative.removeSuffix(".lua").replace('/', '.')}")
                }
            }
        }
        return ScriptSearchResult(request.query, names.filter {
            it.contains(request.query, ignoreCase = true)
        }.take(50))
    }

    fun pendingChanges(): PendingChanges {
        val enabledBundles = resources.listBundles().toSet()
        val paths = registryProvider.getRegistries().values.filterIsInstance<FileBasedRegistry<*>>()
            .flatMap { registry -> registry.getDirtyEntries().values.filter { path ->
                val bundle = path.substringBefore('/')
                bundle in enabledBundles && registry.getResourceIdentifier(bundle, path) != null
            } }.distinct().sorted()
        return PendingChanges(paths)
    }

    fun bundles(): List<String> = resources.listBundles()

    fun registries(bundle: String): List<Identifier> {
        requireEnabledBundle(bundle)
        return registryProvider.getRegistries().mapNotNull { (name, registry) ->
            if (registry is FileBasedRegistry<*> && files(bundle, registry).isNotEmpty()) name else null
        }
    }

    fun project(bundle: String, registry: Identifier): ResourceProject {
        requireEnabledBundle(bundle)
        val fileRegistry = fileRegistry(registry)
        val files = files(bundle, fileRegistry).map { path ->
            val identifier = requireNotNull(fileRegistry.getResourceIdentifier(bundle, path))
            val data = runCatching {
                json.parseToJsonElement(read(bundle, registry, identifier).contents) as? JsonObject
            }.getOrNull()
            val label = data.string("name") ?: (data?.get("metadata") as? JsonObject).string("name")
            ProjectFile(path, label)
        }
        return ResourceProject(bundle, registry.toString(), files, findSchema(registry), files.size)
    }

    fun read(bundle: String, registry: Identifier, identifier: Identifier): ResourceFile {
        requireEnabledBundle(bundle)
        val fileRegistry = fileRegistry(registry)
        val path = fileRegistry.getResourcePath(bundle, identifier)
        val element = if (fileRegistry.getSourcePath(identifier) == path) fileRegistry.getEntryElement(identifier) else null
        val contents = element?.let(::encode) ?: resources.loadAsString(path)
        checkSize(contents)
        return ResourceFile(path, contents, registry.toString(), identifier.toString())
    }

    fun update(bundle: String, registry: Identifier, identifier: Identifier, contents: String): ResourceUpdated {
        requireEnabledBundle(bundle)
        val fileRegistry = fileRegistry(registry)
        val path = fileRegistry.getResourcePath(bundle, identifier)
        fileRegistry.upsertEntry(identifier, parseContents(contents), sourcePath = path)
        return ResourceUpdated(path, pendingChanges())
    }

    fun create(bundle: String, registry: Identifier, identifier: Identifier, contents: String = "{}\n"): ResourceCreated {
        requireEnabledBundle(bundle)
        val fileRegistry = fileRegistry(registry)
        val path = fileRegistry.getResourcePath(bundle, identifier)
        require(!resources.fileExists(path) && fileRegistry.get(identifier) == null) {
            "An entry with this name already exists."
        }
        parseContents(contents)
        return ResourceCreated(bundle, registry.toString(), path, contents, identifier.toString())
    }

    fun persist(bundle: String, registry: Identifier, identifier: Identifier): ChangesResult =
        change(bundle, registry, identifier, ::persistResource)

    fun discard(bundle: String, registry: Identifier, identifier: Identifier): ChangesResult =
        change(bundle, registry, identifier, ::discardResource)

    fun persist(): ChangesResult = changeAll(::persistResource)

    fun discard(): ChangesResult = changeAll(::discardResource)

    private fun change(
        bundle: String, registry: Identifier, identifier: Identifier,
        action: (String, Identifier, Identifier) -> String?
    ): ChangesResult {
        requireEnabledBundle(bundle)
        val fileRegistry = fileRegistry(registry)
        val path = fileRegistry.getResourcePath(bundle, identifier)
        require(fileRegistry.getDirtyEntries()[identifier] == path) { "This resource has no pending changes." }
        return try {
            val removed = action(bundle, registry, identifier)
            ChangesResult(path, pendingChanges(), listOfNotNull(removed))
        } catch (error: Exception) {
            ChangesResult(path, pendingChanges(), error = error.message ?: "Resource operation failed")
        }
    }

    private fun changeAll(action: (String, Identifier, Identifier) -> String?): ChangesResult {
        val removed = mutableListOf<String>()
        val enabledBundles = resources.listBundles().toSet()
        try {
            for ((name, registry) in registryProvider.getRegistries()) {
                if (registry !is FileBasedRegistry<*>) continue
                for ((identifier, path) in registry.getDirtyEntries().entries.sortedBy { it.value }) {
                    val bundle = path.substringBefore('/')
                    if (bundle !in enabledBundles || registry.getResourceIdentifier(bundle, path) == null) continue
                    action(bundle, name, identifier)?.let(removed::add)
                }
            }
        } catch (error: Exception) {
            return ChangesResult(null, pendingChanges(), removed, error.message ?: "Resource operation failed")
        }
        return ChangesResult(null, pendingChanges(), removed)
    }

    private fun persistResource(bundle: String, registry: Identifier, identifier: Identifier): String? {
        val fileRegistry = fileRegistry(registry)
        val path = fileRegistry.getResourcePath(bundle, identifier)
        val data = fileRegistry.getEntryElement(identifier)
        if (data == null) {
            if (resources.fileExists(path)) resources.removeFile(path)
        } else {
            require(fileRegistry.getSourcePath(identifier) == path) { "Resource is no longer the active registry entry." }
            val contents = encode(data)
            checkSize(contents)
            if (resources.fileExists(path)) resources.saveAsString(path, contents) else resources.createAsString(path, contents)
        }
        fileRegistry.markPersisted(identifier)
        return if (data == null) path else null
    }

    private fun discardResource(bundle: String, registry: Identifier, identifier: Identifier): String? {
        val fileRegistry = fileRegistry(registry)
        val path = fileRegistry.getResourcePath(bundle, identifier)
        val exists = resources.fileExists(path)
        if (exists) fileRegistry.upsertEntry(identifier, parseContents(resources.loadAsString(path)), sourcePath = path)
        else fileRegistry.removeEntry(identifier)
        fileRegistry.markPersisted(identifier)
        return if (exists) null else path
    }

    private fun files(bundle: String, registry: FileBasedRegistry<*>): List<String> {
        val paths = resources.listRegistryFiles(bundle, registry) +
            registry.getAll().keys.mapNotNull { registry.getSourcePath(it) }
        return paths.filter { registry.getResourceIdentifier(bundle, it) != null }.distinct().sorted()
    }

    internal fun <T> withResource(path: String, action: (String, Identifier, Identifier) -> T): T {
        val bundle = path.substringBefore('/')
        val candidates = registryProvider.getRegistries().mapNotNull { (name, registry) ->
            val identifier = (registry as? com.seleneworlds.common.data.json.FileBasedRegistry<*>)
                ?.getResourceIdentifier(bundle, path) ?: return@mapNotNull null
            name to identifier
        }
        val resource = candidates.singleOrNull() ?: candidates.singleOrNull { (registry, identifier) ->
            registry.namespace == identifier.namespace
        }
        requireNotNull(resource) { "Unknown or ambiguous registry resource path" }
        return action(bundle, resource.first, resource.second)
    }

    internal fun isAllowed(operation: String, request: JsonObject, check: (String) -> Boolean): Boolean =
        permissionKeys(operation, request).all(check)

    /** Permission keys are derived from the target, never from a caller-supplied permission. */
    internal fun permissionKeys(operation: String, request: JsonObject): List<String> {
        fun resourceKey(path: String, action: String) = withResource(path) { bundle, registry, _ ->
            "$bundle.${registry.path}.$action"
        }
        fun selectionKey(action: String): String {
            val bundle = requireNotNull(request.string("bundle")) { "Missing bundle" }
            val registry = Identifier.parse(requireNotNull(request.string("registry")) { "Missing registry" })
            return "$bundle.${registry.path}.$action"
        }
        return when (operation) {
            "request-project" -> listOf(selectionKey("read"))
            "create-file" -> listOf(selectionKey("edit"))
            "open-file", "save-file" -> listOf(resourceKey(
                requireNotNull(request.string("path")) { "Missing resource path" },
                if (operation == "open-file") "read" else "edit"
            ))
            "persist-changes", "discard-changes", "pending-changes" -> {
                val action = when (operation) {
                    "pending-changes" -> "read"
                    "persist-changes" -> "persist"
                    else -> "edit"
                }
                val path = request.string("path")
                if (path != null) listOf(resourceKey(path, action))
                else pendingChanges().paths.map { resourceKey(it, action) }.distinct()
            }
            "request-bundles" -> listOf("selene.resources.read")
            "request-bundle-registries" -> listOf("${requireNotNull(request.string("bundle"))}.registries.read")
            "search-registry" -> {
                val registry = Identifier.parse(requireNotNull(request.string("registry")))
                listOf("${registry.namespace}.${registry.path}.read")
            }
            "search-scripts" -> listOf("selene.scripts.read")
            else -> throw IllegalArgumentException("Unknown resource operation: $operation")
        }
    }

    private fun findSchema(registry: Identifier): JsonObject? {
        var schema: JsonObject? = null
        var customDefinition = false
        for (bundle in resources.listBundles()) {
            val individual = readObject("$bundle/common/data/${registry.namespace}/registries/${registry.path}.json")
            val collection = readObject("$bundle/common/data/${registry.namespace}/registries.json")
            val aggregated = (collection?.get("entries") as? JsonObject)?.get(registry.path) as? JsonObject
            for (definition in listOfNotNull(individual, aggregated)) {
                customDefinition = true
                (definition["schema"] as? JsonObject)?.let { schema = it }
            }
        }
        return schema ?: if (customDefinition) null else
            builtinSchemas[registry.path] as? JsonObject
    }

    private fun readObject(path: String): JsonObject? {
        if (!resources.fileExists(path)) return null
        return runCatching { json.parseToJsonElement(resources.loadAsString(path)) as? JsonObject }.getOrNull()
    }

    private fun fileRegistry(registry: Identifier): FileBasedRegistry<*> =
        registryProvider.getRegistry(registry) as? FileBasedRegistry<*>
            ?: throw IllegalArgumentException("Unknown or non-editable registry: $registry")

    private fun requireEnabledBundle(bundle: String) {
        require(bundle in resources.listBundles()) { "Unknown or disabled bundle." }
    }

    private fun parseContents(contents: String): JsonObject {
        checkSize(contents)
        return json.parseToJsonElement(contents) as? JsonObject
            ?: throw IllegalArgumentException("Entry contents must be a JSON object.")
    }

    private fun encode(element: JsonElement) = resourceJson.encodeToString(element) + "\n"

    private fun checkSize(contents: String) {
        require(contents.toByteArray(Charsets.UTF_8).size <= MAX_RESOURCE_BYTES) { "Resource exceeds the 4 MiB limit." }
    }

    private fun JsonObject?.string(key: String) = (this?.get(key) as? JsonPrimitive)?.contentOrNull

    companion object {
        private const val MAX_RESOURCE_BYTES = 4 * 1024 * 1024
    }
}

@Serializable
data class RegistrySearchRequest(val registry: String, val query: String, val lookup: Boolean = false)

@Serializable
data class RegistryOption(val value: String, val label: String, val visual: String?)

@Serializable
data class RegistrySearchResult(
    val registry: String,
    val query: String,
    val lookup: Boolean,
    val options: List<RegistryOption>
)

@Serializable
data class ScriptSearchRequest(val query: String)

@Serializable
data class ScriptSearchResult(val query: String, val options: List<String>)

internal fun Route.registerEditorResourceRoutes(
    editorResources: EditorResources,
    permissions: PermissionsApi,
    playerManager: PlayerManager,
    mainThreadDispatcher: MainThreadDispatcher,
    logger: Logger,
    authenticatedUser: (ApplicationCall) -> SeleneUser
) {
    suspend fun execute(
        call: ApplicationCall,
        operation: String,
        receiveRequest: suspend () -> JsonObject,
        handler: (JsonObject) -> String
    ) {
        call.response.header(HttpHeaders.CacheControl, "no-store")
        val request = try {
            receiveRequest()
        } catch (_: IllegalArgumentException) {
            call.respond(HttpStatusCode.BadRequest, "Invalid resource request")
            return
        }
        val context = seleneJson.decodeFromJsonElement(SerializedMapSerializer, request)
        val user = authenticatedUser(call)
        val result = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            mainThreadDispatcher.callOnMainThread {
                val player = playerManager.dereferencePersisted(user.userId)
                try {
                    val allowed = player != null && editorResources.isAllowed(operation, request) { key ->
                        permissions.hasPermission(
                            player.api, key, mapOf("operation" to operation, "request" to context)
                        )
                    }
                    if (!allowed) {
                        return@callOnMainThread 403 to "{\"message\":\"Resource access denied\"}"
                    }
                    200 to handler(request)
                } catch (error: IllegalArgumentException) {
                    400 to seleneJson.encodeToString(
                        mapOf("message" to (error.message ?: "Invalid resource request"))
                    )
                } catch (error: Exception) {
                    logger.error("Resource HTTP operation failed: $operation", error)
                    500 to "{\"message\":\"Resource operation failed\"}"
                }
            }
        }
        call.respondText(result.second, ContentType.Application.Json, HttpStatusCode.fromValue(result.first))
    }

    post("/resources/permissions") {
        call.response.header(HttpHeaders.CacheControl, "no-store")
        val requests = try {
            seleneJson.decodeFromString<ResourcePermissionChecks>(call.receiveText()).checks.also { requests ->
                require(requests.size <= 32) { "Too many permission checks" }
                require(requests.all { it.operation in setOf(
                    "create-file", "save-file", "persist-changes", "discard-changes"
                ) }) { "Unsupported permission operation" }
            }
        } catch (_: IllegalArgumentException) {
            call.respond(HttpStatusCode.BadRequest, "Invalid permission request")
            return@post
        }
        val user = authenticatedUser(call)
        val allowed = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            mainThreadDispatcher.callOnMainThread {
                val player = playerManager.dereferencePersisted(user.userId)
                requests.map { request ->
                    player != null && runCatching {
                        val context = seleneJson.decodeFromJsonElement(SerializedMapSerializer, request.request)
                        editorResources.isAllowed(request.operation, request.request) { key ->
                            permissions.hasPermission(player.api, key,
                                mapOf("operation" to request.operation, "request" to context))
                        }
                    }.getOrDefault(false)
                }
            }
        }
        call.respondText(seleneJson.encodeToString(allowed), ContentType.Application.Json)
    }

    get("/registries/{registry}/entries") {
        execute(call, "search-registry", { call.resourceQuery() }) { payload ->
            val request = seleneJson.decodeFromJsonElement<RegistrySearchRequest>(payload)
            seleneJson.encodeToString(editorResources.searchRegistry(request))
        }
    }
    get("/scripts") {
        execute(call, "search-scripts", { call.resourceQuery() }) { payload ->
            val request = seleneJson.decodeFromJsonElement<ScriptSearchRequest>(payload)
            seleneJson.encodeToString(editorResources.searchScripts(request))
        }
    }
    get("/resources/bundles") {
        execute(call, "request-bundles", { buildJsonObject {} }) {
            seleneJson.encodeToString(BundleOptions(editorResources.bundles()))
        }
    }
    get("/resources/bundles/{bundle}/registries") {
        execute(call, "request-bundle-registries", { call.resourceQuery() }) { payload ->
            val request = seleneJson.decodeFromJsonElement<BundleSelection>(payload)
            seleneJson.encodeToString(BundleRegistryOptions(request.bundle,
                editorResources.registries(request.bundle).map { it.toString() }))
        }
    }
    get("/resources/bundles/{bundle}/registries/{registry}") {
        execute(call, "request-project", { call.resourceQuery() }) { payload ->
            val request = seleneJson.decodeFromJsonElement<ProjectSelection>(payload)
            seleneJson.encodeToString(editorResources.project(request.bundle, Identifier.parse(request.registry)))
        }
    }
    get("/resources/files/{path...}") {
        execute(call, "open-file", { call.resourceQuery() }) { payload ->
            val request = seleneJson.decodeFromJsonElement<ResourcePathRequest>(payload)
            seleneJson.encodeToString(editorResources.withResource(request.path, editorResources::read))
        }
    }
    post("/resources/bundles/{bundle}/registries/{registry}/files") {
        execute(call, "create-file", { call.resourceBody() }) { payload ->
            val request = seleneJson.decodeFromJsonElement<CreateResourceRequest>(payload)
            val registry = Identifier.parse(request.registry)
            require(request.name.trim().matches(Regex("[A-Za-z0-9_-]+"))) { "Invalid entry name" }
            val identifier = Identifier(request.namespace ?: registry.namespace, request.name.trim())
            if (request.sourcePath != null) {
                editorResources.withResource(request.sourcePath) { sourceBundle, sourceRegistry, _ ->
                    require(sourceBundle == request.bundle && sourceRegistry == registry) { "Source entry belongs to another selection." }
                }
            }
            seleneJson.encodeToString(editorResources.create(request.bundle, registry, identifier, request.contents ?: "{}\n"))
        }
    }
    put("/resources/files/{path...}") {
        execute(call, "save-file", { call.resourceBody() }) { payload ->
            val request = seleneJson.decodeFromJsonElement<UpdateResourceRequest>(payload)
            seleneJson.encodeToString(editorResources.withResource(request.path) { bundle, registry, identifier ->
                editorResources.update(bundle, registry, identifier, request.contents)
            })
        }
    }
    post("/resources/changes/persist") {
        execute(call, "persist-changes", { call.resourceBody() }) { payload ->
            val request = seleneJson.decodeFromJsonElement<ChangeRequest>(payload)
            seleneJson.encodeToString(if (request.path == null) editorResources.persist() else editorResources.withResource(request.path, editorResources::persist))
        }
    }
    post("/resources/changes/persist/{path...}") {
        execute(call, "persist-changes", { call.resourceBody() }) { payload ->
            val request = seleneJson.decodeFromJsonElement<ChangeRequest>(payload)
            seleneJson.encodeToString(if (request.path == null) editorResources.persist() else editorResources.withResource(request.path, editorResources::persist))
        }
    }
    post("/resources/changes/discard") {
        execute(call, "discard-changes", { call.resourceBody() }) { payload ->
            val request = seleneJson.decodeFromJsonElement<ChangeRequest>(payload)
            seleneJson.encodeToString(if (request.path == null) editorResources.discard() else editorResources.withResource(request.path, editorResources::discard))
        }
    }
    post("/resources/changes/discard/{path...}") {
        execute(call, "discard-changes", { call.resourceBody() }) { payload ->
            val request = seleneJson.decodeFromJsonElement<ChangeRequest>(payload)
            seleneJson.encodeToString(if (request.path == null) editorResources.discard() else editorResources.withResource(request.path, editorResources::discard))
        }
    }
    get("/resources/changes") {
        execute(call, "pending-changes", { buildJsonObject {} }) {
            seleneJson.encodeToString(editorResources.pendingChanges())
        }
    }
}

private fun RoutingCall.resourceParameters(): JsonObject = buildJsonObject {
    for (name in listOf("bundle", "registry")) {
        pathParameters[name]?.let { put(name, it) }
    }
    pathParameters.getAll("path")?.takeIf { it.isNotEmpty() }?.let { put("path", it.joinToString("/")) }
}

private fun RoutingCall.resourceQuery(): JsonObject = buildJsonObject {
    for (name in listOf("query", "lookup")) {
        val value = request.queryParameters[name] ?: continue
        if (name == "lookup") {
            put(name, JsonPrimitive(requireNotNull(value.toBooleanStrictOrNull()) { "Invalid lookup value" }))
        } else {
            put(name, value)
        }
    }
    for ((name, value) in resourceParameters()) put(name, value)
}

private suspend fun RoutingCall.resourceBody(): JsonObject = buildJsonObject {
    for ((name, value) in seleneJson.parseToJsonElement(receiveText()).jsonObject) put(name, value)
    for ((name, value) in resourceParameters()) put(name, value)
}

@Serializable
data class ResourcePathRequest(val path: String)

@Serializable
data class ChangeRequest(val path: String? = null)

@Serializable
data class ProjectSelection(val bundle: String, val registry: String)

@Serializable
data class CreateResourceRequest(
    val bundle: String,
    val registry: String,
    val name: String,
    val sourcePath: String? = null,
    val contents: String? = null,
    val namespace: String? = null
)

@Serializable
data class UpdateResourceRequest(val path: String, val contents: String)

@Serializable
data class ResourceFile(val path: String, val contents: String, val registry: String, val identifier: String)

@Serializable
data class ProjectFile(val path: String, val name: String?)

@Serializable
data class BundleOptions(val bundles: List<String>)

@Serializable
data class BundleSelection(val bundle: String)

@Serializable
data class BundleRegistryOptions(val bundle: String, val registries: List<String>)

@Serializable
data class ResourceProject(
    val bundle: String,
    val registry: String,
    val files: List<ProjectFile>,
    val schema: JsonObject?,
    val count: Int
)

@Serializable
data class PendingChanges(val paths: List<String>, val count: Int) {
    constructor(paths: List<String>) : this(paths, paths.size)
}

@Serializable
data class ResourceUpdated(val path: String, val pendingChanges: PendingChanges)

@Serializable
data class ResourceCreated(val bundle: String, val registry: String, val path: String, val contents: String, val identifier: String)

@Serializable
data class ChangesResult(
    val path: String?,
    val pendingChanges: PendingChanges,
    val removedPaths: List<String> = emptyList(),
    val error: String? = null
)

@Serializable
internal data class ResourcePermissionRequest(val operation: String, val request: JsonObject)

@Serializable
internal data class ResourcePermissionChecks(val checks: List<ResourcePermissionRequest>)
