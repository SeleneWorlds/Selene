package com.seleneworlds.server.http

import com.seleneworlds.common.bundles.*
import com.seleneworlds.common.data.*
import com.seleneworlds.common.i18n.Messages
import org.slf4j.LoggerFactory
import java.util.Locale
import com.seleneworlds.common.data.custom.*
import com.seleneworlds.common.serialization.seleneJson
import kotlinx.serialization.json.*
import java.nio.file.Files
import io.ktor.http.Parameters
import kotlin.test.*

class EditorResourcesTest {
    @Test
    fun `script schema query retains module and function name`() {
        val module = "illarion-script-loader.server.lua.lib.behaviorDefaults"
        for (functionName in listOf("schema", "actionSchema")) {
            val payload = parseResourceQuery(Parameters.build {
                append("module", module)
                append("functionName", functionName)
            }, buildJsonObject {})
            assertEquals(buildJsonObject {
                put("module", module)
                put("functionName", functionName)
            }, payload)
        }
        val payload = parseResourceQuery(Parameters.build {
            append("module", module)
        }, buildJsonObject {})
        assertEquals(JsonPrimitive(module), payload["module"])
        assertNull(payload["functionName"])
    }

    @Test
    fun `registry schemas resolve shared types recursively and preserve field options`() = fixture { api, resources, _ ->
        resources.createAsString("active/common/data/test/registries/items.json",
            """{"schema":{"drops":{"type":"moonlight:rolls","optional":true}}}""", createDirectories = true)
        resources.createAsString("active/common/data/moonlight/schemas/rolls.json",
            """{"definition":{"type":"object","editor":"moonlight:rolls","properties":{"pools":{"type":"array","valueType":"test:pool"}}}}""",
            createDirectories = true)
        resources.createAsString("active/common/data/test/schemas.json",
            """{"entries":{"pool":{"definition":{"type":"object","properties":{"rolls":"integer","data":{"type":"schema_lookup","variants":{"item":"test:item"}}}}},"item":{"definition":{"type":"object","properties":{"name":"string"}}}}}""",
            createDirectories = true)
        val drops = api.project("active", registryName).schema!!.getValue("drops").jsonObject
        assertEquals(JsonPrimitive(true), drops["optional"])
        assertEquals(JsonPrimitive("moonlight:rolls"), drops["editor"])
        val pool = drops["properties"]!!.jsonObject["pools"]!!.jsonObject["valueType"]!!.jsonObject
        assertEquals(JsonPrimitive("integer"), pool["properties"]!!.jsonObject["rolls"])
        val item = pool["properties"]!!.jsonObject["data"]!!.jsonObject["variants"]!!.jsonObject["item"]!!.jsonObject
        assertEquals(JsonPrimitive("string"), item["properties"]!!.jsonObject["name"])
        resources.saveAsString("active/common/data/moonlight/schemas/rolls.json",
            """{"definition":{"type":"object","properties":{"nested":"moonlight:rolls"}}}""")
        assertFailsWith<IllegalArgumentException> { api.project("active", registryName) }
    }

    @Test
    fun `named schemas accept static parameters without changing shared definitions`() = fixture { api, resources, _ ->
        resources.createAsString("active/common/data/test/registries/items.json",
            """{"schema":{"drops":{"type":"test:table","optional":true,"parameters":{"entryTypes":{"item":"test:payload","nothing":"any"},"conditionTypes":{"level":"integer"}}},"plain":"test:table"}}""", createDirectories = true)
        resources.createAsString("active/common/data/test/schemas/table.json",
            """{"definition":{"type":"object","editor":"test:table","properties":{"conditions":{"type":"array","valueType":{"type":"object","properties":{"type":{"${'$'}parameter":"conditionTypes","transform":"enum","default":"string"},"data":{"type":"schema_lookup","property":"type","variants":{"${'$'}parameter":"conditionTypes","default":{}}}}}},"pools":{"type":"array","valueType":{"type":"object","properties":{"entries":{"type":"array","valueType":{"type":"object","properties":{"type":{"${'$'}parameter":"entryTypes","transform":"enum","default":"string"},"weight":"number","data":{"type":"schema_lookup","property":"type","variants":{"${'$'}parameter":"entryTypes","default":{}}}}}}}}}}}}""", createDirectories = true)
        resources.createAsString("active/common/data/test/schemas/payload.json",
            """{"definition":{"type":"object","properties":{"item":"string"}}}""", createDirectories = true)
        val schema = api.project("active", registryName).schema!!
        fun entry(field: String) = schema[field]!!.jsonObject["properties"]!!.jsonObject["pools"]!!.jsonObject["valueType"]!!.jsonObject["properties"]!!.jsonObject["entries"]!!.jsonObject["valueType"]!!.jsonObject["properties"]!!.jsonObject
        val payload = entry("drops")["data"]!!.jsonObject
        assertEquals(JsonPrimitive("schema_lookup"), payload["type"])
        assertEquals(JsonPrimitive("string"), payload["variants"]!!.jsonObject["item"]!!.jsonObject["properties"]!!.jsonObject["item"])
        assertEquals(JsonPrimitive("string"), entry("plain")["type"])
        assertNull(schema["drops"]!!.jsonObject["parameters"])
        val condition = schema["drops"]!!.jsonObject["properties"]!!.jsonObject["conditions"]!!.jsonObject["valueType"]!!.jsonObject["properties"]!!.jsonObject
        assertEquals(JsonPrimitive("integer"), condition["data"]!!.jsonObject["variants"]!!.jsonObject["level"])
    }

    @Test
    fun `localization edits preserve properties and reject stale or invalid writes`() = fixture { api, resources, _ ->
        val path = "active/server/i18n/messages_de.properties"
        val original = "# Greeting\nhello=Grüße {name}\n"
        resources.createAsString(path, original, createDirectories = true)
        resources.createAsString("active/server/i18n/other.properties", "hidden=value\n")
        assertEquals(listOf(path), api.localizationFiles("active"))
        assertEquals(original, api.readLocalization(path).contents)
        val updated = original + "farewell=Auf Wiedersehen\n"
        assertEquals(updated, api.saveLocalization(path, updated, original).contents)
        assertEquals(updated, resources.loadAsString(path))
        assertFailsWith<IllegalArgumentException> { api.saveLocalization(path, original, original) }
        assertFailsWith<IllegalArgumentException> { api.saveLocalization(path, "bad=\\uXXXX", updated) }
        assertEquals(updated, resources.loadAsString(path))
        for (invalid in listOf("active/server/i18n/../messages_de.properties", "active/server/lua/messages_de.properties",
            "disabled/server/i18n/messages_de.properties", "active/server/i18n/other.properties")) {
            assertFailsWith<IllegalArgumentException> { api.readLocalization(invalid) }
        }
        val request = buildJsonObject { put("path", path) }
        assertEquals(listOf("active.localization.read"), api.permissionKeys("read-localization", request))
        assertEquals(listOf("active.localization.persist"), api.permissionKeys("save-localization", request))
        assertFalse(api.isAllowed("save-localization", request) { it.endsWith(".read") })
    }

    @Test
    fun `saving localization refreshes cached server lookups immediately`() {
        lateinit var messages: Messages
        fixture(configureMessages = { messages = it }) { api, resources, _ ->
            val path = "active/server/i18n/messages_de.properties"
            val original = "greeting=Hallo\nremoved=Alt\n"
            resources.createAsString(path, original, createDirectories = true)
            assertEquals("Hallo", messages.get("greeting", Locale.GERMAN))
            api.saveLocalization(path, "greeting=Grüße\nadded=Neu\n", original)
            assertEquals("Grüße", messages.get("greeting", Locale.GERMAN))
            assertEquals("Neu", messages.get("added", Locale.GERMAN))
            assertNull(messages.get("removed", Locale.GERMAN))
        }
    }

    private val registryName = Identifier("test", "items")
    private val entry = Identifier("test", "example")
    private val path = "active/common/data/test/items/example.json"

    private fun fixture(
        registryIdentifier: Identifier = registryName,
        includeEmptyBundle: Boolean = false,
        platform: String = "common",
        configureMessages: (Messages) -> Unit = {},
        test: (EditorResources, ResourcesApi, CustomRegistry) -> Unit
    ) {
        val root = Files.createTempDirectory("http-resources")
        try {
            val data = root.resolve("common/data/test/items")
            Files.createDirectories(data)
            Files.writeString(data.resolve("example.json"), "{\"name\":\"Example\"}")
            val database = BundleDatabase().apply {
                addBundle(Bundle(BundleManifest("active"), root.toFile()))
                addBundle(Bundle(BundleManifest("disabled"), root.toFile()), false)
                if (includeEmptyBundle) {
                    addBundle(Bundle(BundleManifest("empty"), root.resolve("empty").toFile()))
                }
            }
            val registry = CustomRegistry(seleneJson, CustomRegistryDefinition("items", platform))
            registry.load(database)
            val provider = object : RegistryProvider {
                override fun getRegistries(): Map<Identifier, Registry<*>> = mapOf(registryIdentifier to registry)
                override fun getRegistry(identifier: Identifier): Registry<*>? = registry.takeIf { identifier == registryIdentifier }
            }
            val resources = ResourcesApi(database)
            val messages = Messages(database, LoggerFactory.getLogger(Messages::class.java))
            configureMessages(messages)
            val editor = EditorResources(resources, provider, seleneJson, database, messages)
            test(editor, resources, registry)
        } finally {
            root.toFile().deleteRecursively()
        }
    }

    @Test
    fun `metadata search returns the registry icon from individual and aggregated definitions`() = fixture { api, resources, _ ->
        val request = ResourceMetadataSearchRequest("active", registryName.toString(), "category")
        api.update("active", registryName, entry, """{"metadata":{"category":"quest"}}""")
        assertNull(api.searchMetadata(request).icon)
        resources.createAsString("active/common/data/test/registries/items.json",
            """{"name":"items","platform":"common","icon":"map-pin","schema":{"enabled":"enabled"}}""",
            createDirectories = true)
        assertEquals("map-pin", api.searchMetadata(request).icon)
        resources.createAsString("active/common/data/test/registries.json",
            """{"entries":{"items":{"icon":"DoorOpen"}}}""")
        val result = api.searchMetadata(request)
        assertEquals("DoorOpen", result.icon)
        assertEquals(buildJsonObject { put("enabled", "enabled") }, result.schema)
        assertEquals("map-pin", seleneJson.decodeFromString<CustomRegistryDefinition>(
            resources.loadAsString("active/common/data/test/registries/items.json")).icon)
    }

    @Test
    fun `metadata search can project fields by schema type`() = fixture { api, resources, _ ->
        resources.createAsString("active/common/data/test/registries.json",
            """{"entries":{"items":{"schema":{"active":{"type":"enabled"},"script":"script"}}}}""")
        api.update("active", registryName, entry,
            """{"active":true,"script":"large","metadata":{"category":"quest"}}""")
        val request = ResourceMetadataSearchRequest("active", registryName.toString(), "category", fieldType = "enabled")
        assertEquals(buildJsonObject { put("active", true) }, api.searchMetadata(request).files.single().fields)
        assertTrue(api.searchMetadata(request.copy(fieldType = null)).files.single().fields.isEmpty())
    }

    @Test
    fun `metadata searches return matching resources without their contents`() = fixture { api, _, _ ->
        val request = ResourceMetadataSearchRequest("active", registryName.toString(), "category")
        assertTrue(api.searchMetadata(request).files.isEmpty())
        api.update("active", registryName, entry,
            """{"name":"Example","metadata":{"category":"quest","secret":"excluded"},"script":"large"}""")
        val result = api.searchMetadata(request)
        assertEquals(listOf(path), result.files.map { it.path })
        assertEquals("Example", result.files.single().name)
        assertEquals(buildJsonObject { put("category", "quest") }, result.files.single().metadata)
        assertEquals(result, api.searchMetadata(request.copy(value = "quest")))
        assertTrue(api.searchMetadata(request.copy(value = "other")).files.isEmpty())
        assertEquals(listOf("active.items.read"), api.permissionKeys("search-resource-metadata", buildJsonObject {
            put("bundle", "active"); put("registry", registryName.toString())
        }))
        assertFailsWith<IllegalArgumentException> { api.searchMetadata(request.copy(bundle = "disabled")) }
        assertFailsWith<IllegalArgumentException> { api.searchMetadata(request.copy(key = "")) }
        val encoded = seleneJson.encodeToString(ResourceMetadataSearchResult.serializer(), result)
        assertFalse(encoded.contains("script"))
        assertFalse(encoded.contains("contents"))
        assertFalse(encoded.contains("secret"))
    }

    @Test
    fun `field option query parameters survive request parsing`() {
        val payload = parseResourceQuery(Parameters.build {
            append("value", "test:example")
            append("property", "types")
            append("registry", "test:wrong")
        }, buildJsonObject { put("registry", "test:items") })
        assertEquals(RegistryFieldRequest("test:items", "test:example", "types"),
            seleneJson.decodeFromJsonElement<RegistryFieldRequest>(payload))
        val metadata = parseResourceQuery(Parameters.build {
            append("key", "category")
            append("value", "quest")
        }, buildJsonObject { put("bundle", "active"); put("registry", "test:items") })
        assertEquals(ResourceMetadataSearchRequest("active", "test:items", "category", "quest"),
            seleneJson.decodeFromJsonElement<ResourceMetadataSearchRequest>(metadata))
        val lookup = parseResourceQuery(Parameters.build {
            append("query", "example")
            append("lookup", "true")
        }, buildJsonObject { put("registry", "test:items") })
        assertEquals(JsonPrimitive(true), lookup["lookup"])
        assertEquals(JsonPrimitive("example"), lookup["query"])
    }

    @Test
    fun `registry field references infer map key values from the schema`() = fixture { api, resources, _ ->
        val schemaPath = "active/common/data/test/registries.json"
        resources.createAsString(schemaPath,
            """{"entries":{"items":{"schema":{"types":{"type":"map","keyType":"integer"}}}}}""")
        api.update("active", registryName, entry, """{"types":{"10":{},"2":{},"0":{}}}""")
        val request = RegistryFieldRequest("test:items", "test:example", "types")
        val options = api.registryFieldOptions(request).getValue("options").jsonArray
        assertEquals(listOf(0, 2, 10), options.map { it.jsonObject.getValue("value").jsonPrimitive.int })
        assertTrue(options.all { !it.jsonObject.getValue("value").jsonPrimitive.isString })
        resources.saveAsString(schemaPath,
            """{"entries":{"items":{"schema":{"types":{"type":"map","keyType":"string"}}}}}""")
        assertTrue(api.registryFieldOptions(request).getValue("options").jsonArray.all {
            it.jsonObject.getValue("value").jsonPrimitive.isString
        })
        api.update("active", registryName, entry, "{}")
        assertEquals(JsonArray(emptyList()), api.registryFieldOptions(request)["options"])
        assertFailsWith<IllegalArgumentException> { api.registryFieldOptions(request.copy(property = "unknown")) }
        assertFailsWith<IllegalArgumentException> { api.registryFieldOptions(request.copy(value = "test:missing")) }
        api.update("active", registryName, entry, """{"types":3}""")
        assertFailsWith<IllegalArgumentException> { api.registryFieldOptions(request) }
        assertEquals(listOf("test.items.read"), api.permissionKeys("registry-field-options", buildJsonObject {
            put("registry", "test:items")
        }))
    }

    @Test
    fun `resource permissions use the selected bundle registry and action`() = fixture { api, _, _ ->
        val selection = buildJsonObject {
            put("bundle", "example-data")
            put("registry", "example:npcs")
        }
        assertEquals(listOf("example-data.npcs.edit"), api.permissionKeys("create-file", selection))
        assertEquals(listOf("example-data.npcs.read"), api.permissionKeys("request-project", selection))
        val resource = buildJsonObject { put("path", path) }
        assertEquals(listOf("active.items.read"), api.permissionKeys("open-file", resource))
        assertEquals(listOf("active.items.persist"), api.permissionKeys("persist-changes", resource))
        for (operation in listOf("save-file", "discard-changes")) {
            assertEquals(listOf("active.items.edit"), api.permissionKeys(operation, resource))
        }
        assertFailsWith<IllegalArgumentException> {
            api.permissionKeys("save-file", buildJsonObject { put("path", "active/unknown.json") })
        }
    }

    @Test
    fun `bulk permissions cover every dirty bundle before executing`() =
        fixture(includeEmptyBundle = true) { api, _, _ ->
            api.update("active", registryName, entry, "{}")
            api.update("active", registryName, Identifier("test", "another"), "{}")
            api.update("empty", registryName, Identifier("test", "new"), "{}")
            assertEquals(listOf("active.items.persist", "empty.items.persist"),
                api.permissionKeys("persist-changes", buildJsonObject {}))
            assertEquals(listOf("active.items.edit", "empty.items.edit"),
                api.permissionKeys("discard-changes", buildJsonObject {}))
            assertEquals(listOf("active.items.read", "empty.items.read"),
                api.permissionKeys("pending-changes", buildJsonObject {}))
            assertEquals(3, api.pendingChanges().count)
        }

    @Test
    fun `permission queries separate edit from persist and require every bulk scope`() =
        fixture(includeEmptyBundle = true) { api, _, _ ->
            val resource = buildJsonObject { put("path", path) }
            val editOnly: (String) -> Boolean = { it == "active.items.edit" }
            assertTrue(api.isAllowed("save-file", resource, editOnly))
            assertFalse(api.isAllowed("persist-changes", resource, editOnly))
            assertTrue(api.isAllowed("persist-changes", resource) { it == "active.items.persist" })
            api.update("active", registryName, entry, "{}")
            api.update("empty", registryName, Identifier("test", "new"), "{}")
            assertFalse(api.isAllowed("persist-changes", buildJsonObject {}) { it == "active.items.persist" })
            assertTrue(api.isAllowed("persist-changes", buildJsonObject {}) { it.endsWith(".persist") })
            assertEquals(2, api.pendingChanges().count)
        }

    @Test
    fun `discovery and search have separate read permissions`() = fixture { api, _, _ ->
        assertEquals(listOf("selene.resources.read"), api.permissionKeys("request-bundles", buildJsonObject {}))
        assertEquals(listOf("active.registries.read"), api.permissionKeys("request-bundle-registries",
            buildJsonObject { put("bundle", "active") }))
        assertEquals(listOf("test.items.read"), api.permissionKeys("search-registry",
            buildJsonObject { put("registry", "test:items") }))
        assertEquals(listOf("selene.scripts.read"), api.permissionKeys("search-scripts", buildJsonObject {}))
        assertEquals(listOf("selene.scripts.read"), api.permissionKeys("script-schema", buildJsonObject {}))
    }

    @Test
    fun `built-in schemas are available without the editor bundle`() = fixture(Identifier("selene", "tiles")) { api, _, _ ->
        val schema = assertNotNull(api.project("active", Identifier("selene", "tiles")).schema)
        assertEquals("selene:visuals", schema["visual"]!!.jsonObject["registry"]!!.jsonPrimitive.content)
    }

    @Test
    fun `custom registry schemas override built-in schemas`() = fixture { api, resources, _ ->
        resources.createAsString("active/common/data/test/registries.json", """{"entries":{"items":{"schema":{"custom":"string"}}}}""")
        assertEquals(buildJsonObject { put("custom", "string") }, api.project("active", registryName).schema)
    }

    @Test
    fun `custom registry definitions without schemas suppress the built-in fallback`() = fixture { api, resources, _ ->
        resources.createAsString("active/common/data/test/registries.json", """{"entries":{"items":{}}}""")
        assertNull(api.project("active", registryName).schema)
    }

    @Test
    fun `persisting the first server entry creates missing namespace directories`() = fixture(platform = "server") { api, resources, registry ->
        val identifier = Identifier("moonlight", "new")
        val created = api.create("active", registryName, identifier, """{"name":"New","enabled":false}""")
        assertEquals("active/server/data/moonlight/items/new.json", created.path)
        assertFalse(resources.fileExists(created.path))
        api.update("active", registryName, identifier, created.contents)
        assertEquals(listOf(created.path), api.pendingChanges().paths)
        assertNull(api.persist("active", registryName, identifier).error)
        assertTrue(resources.fileExists(created.path))
        assertEquals(seleneJson.parseToJsonElement(created.contents), seleneJson.parseToJsonElement(resources.loadAsString(created.path)))
        assertFalse(registry.isDirty(identifier))
        assertEquals(0, api.pendingChanges().count)
    }

    @Test
    fun `apply changes the registry and persist changes the file`() = fixture { api, resources, registry ->
        val contents = "{\"name\":\"Updated\",\"text\":\"${"x".repeat(70 * 1024)}\"}"
        api.update("active", registryName, entry, contents)
        assertEquals("Example", seleneJson.parseToJsonElement(resources.loadAsString(path)).jsonObject["name"]!!.jsonPrimitive.content)
        assertEquals("Updated", registry.get(entry)!!.element.jsonObject["name"]!!.jsonPrimitive.content)
        assertEquals(seleneJson.parseToJsonElement(contents), seleneJson.parseToJsonElement(api.read("active", registryName, entry).contents))
        assertNull(api.discard("active", registryName, entry).error)
        assertEquals("Example", registry.get(entry)!!.element.jsonObject["name"]!!.jsonPrimitive.content)
        api.update("active", registryName, entry, contents)
        assertNull(api.persist("active", registryName, entry).error)
        assertEquals(seleneJson.parseToJsonElement(contents), seleneJson.parseToJsonElement(resources.loadAsString(path)))
    }

    @Test
    fun `project listings use the actual registry identifier`() = fixture { api, _, _ ->
        assertEquals(listOf("active"), api.bundles())
        assertEquals(listOf(registryName), api.registries("active"))
        assertFailsWith<IllegalArgumentException> { api.registries("disabled") }
        val project = api.project("active", registryName)
        assertEquals(registryName.toString(), project.registry)
        assertEquals(listOf(ProjectFile(path, "Example")), project.files)
    }

    @Test
    fun `bundle discovery includes empty enabled bundles and registry discovery is scoped`() =
        fixture(includeEmptyBundle = true) { api, _, _ ->
            assertEquals(listOf("active", "empty"), api.bundles())
            assertEquals(listOf(registryName), api.registries("active"))
            assertEquals(emptyList(), api.registries("empty"))
            assertFailsWith<IllegalArgumentException> { api.registries("unknown") }
        }

    @Test
    fun `registry and entry namespaces are independent`() {
        val builtin = Identifier("selene", "items")
        fixture(builtin) { api, _, registry ->
            val resource = api.read("active", builtin, entry)
            assertEquals(path, resource.path)
            assertEquals("selene:items", resource.registry)
            assertEquals("test:example", resource.identifier)
            api.update("active", builtin, entry, "{\"name\":\"Updated\"}")
            assertEquals("Updated", registry.get(entry)!!.element.jsonObject["name"]!!.jsonPrimitive.content)
            assertFailsWith<IllegalArgumentException> { api.read("active", registryName, entry) }
        }
    }

    @Test
    fun `rejects traversal disabled bundles invalid json and oversized updates`() = fixture { api, _, _ ->
        assertFailsWith<IllegalArgumentException> { api.read("disabled", registryName, entry) }
        assertFailsWith<IllegalArgumentException> { api.read("active", registryName, Identifier("test", "../secret")) }
        assertFailsWith<IllegalArgumentException> { api.read("active/../other", registryName, entry) }
        for (contents in listOf("[]", "invalid", " ".repeat(4 * 1024 * 1024 + 1))) {
            assertFailsWith<IllegalArgumentException> { api.update("active", registryName, entry, contents) }
        }
        assertEquals(0, api.pendingChanges().count)
    }

    @Test
    fun `rename stages both paths and preserves contents`() = fixture { api, resources, registry ->
        val renamed = api.rename("active", registryName, entry, "renamed")
        assertNull(registry.get(entry))
        assertNotNull(registry.get(Identifier("test", "renamed")))
        assertEquals(setOf(path, renamed.path), api.pendingChanges().paths.toSet())
        assertNull(api.persist().error)
        assertFalse(resources.fileExists(path))
        assertEquals(seleneJson.parseToJsonElement(renamed.contents), seleneJson.parseToJsonElement(resources.loadAsString(renamed.path)))
    }

    @Test
    fun `rollback restores the original filename`() = fixture { api, resources, registry ->
        val renamed = api.rename("active", registryName, entry, "renamed")
        assertNull(api.discard().error)
        assertNotNull(registry.get(entry))
        assertNull(registry.get(Identifier("test", "renamed")))
        assertTrue(resources.fileExists(path))
        assertFalse(resources.fileExists(renamed.path))
        assertEquals(0, api.pendingChanges().count)
    }

    @Test
    fun `persisting the renamed entry removes the original file`() = fixture { api, resources, _ ->
        val renamed = api.rename("active", registryName, entry, "renamed")
        val result = api.persist("active", registryName, Identifier("test", "renamed"))
        assertNull(result.error)
        assertEquals(listOf(path), result.removedPaths)
        assertFalse(resources.fileExists(path))
        assertTrue(resources.fileExists(renamed.path))
        assertEquals(0, api.pendingChanges().count)
    }

    @Test
    fun `rolling back the renamed entry restores the original entry`() = fixture { api, resources, registry ->
        val renamed = api.rename("active", registryName, entry, "renamed")
        val result = api.discard("active", registryName, Identifier("test", "renamed"))
        assertNull(result.error)
        assertEquals(listOf(renamed.path), result.removedPaths)
        assertNotNull(registry.get(entry))
        assertNull(registry.get(Identifier("test", "renamed")))
        assertTrue(resources.fileExists(path))
        assertFalse(resources.fileExists(renamed.path))
        assertEquals(0, api.pendingChanges().count)
    }

    @Test
    fun `successive renames persist only the final filename`() = fixture { api, resources, _ ->
        val first = api.rename("active", registryName, entry, "first")
        val last = api.rename("active", registryName, Identifier("test", "first"), "last")
        assertNull(api.persist("active", registryName, Identifier("test", "last")).error)
        assertFalse(resources.fileExists(path))
        assertFalse(resources.fileExists(first.path))
        assertTrue(resources.fileExists(last.path))
        assertEquals(0, api.pendingChanges().count)
    }

    @Test
    fun `rename rejects conflicts and invalid names`() = fixture { api, _, registry ->
        for (name in listOf("example", "../other", "")) {
            assertFailsWith<IllegalArgumentException> { api.rename("active", registryName, entry, name) }
        }
        assertNotNull(registry.get(entry))
        assertEquals(0, api.pendingChanges().count)
    }

    @Test
    fun `new resources remain browser drafts until applied`() = fixture { api, resources, registry ->
        val identifier = Identifier("test", "new")
        val draft = api.create("active", registryName, identifier)
        assertFalse(resources.fileExists(draft.path))
        assertNull(registry.get(identifier))
        assertEquals(0, api.pendingChanges().count)
        assertFailsWith<IllegalArgumentException> { api.read("active", registryName, identifier) }
        api.update("active", registryName, identifier, draft.contents)
        assertNotNull(registry.get(identifier))
        assertFailsWith<IllegalArgumentException> { api.create("active", registryName, identifier) }
        assertEquals(listOf(draft.path), api.pendingChanges().paths)
        assertTrue(api.project("active", registryName).files.any { it.path == draft.path })
        assertNull(api.discard("active", registryName, identifier).error)
        assertNull(registry.get(identifier))
        api.update("active", registryName, identifier, "{}")
        assertNull(api.persist("active", registryName, identifier).error)
        assertTrue(resources.fileExists(draft.path))
    }

    @Test
    fun `persist uses the latest registry entry`() = fixture { api, resources, registry ->
        api.update("active", registryName, entry, "{\"name\":\"Applied\"}")
        val latest = seleneJson.parseToJsonElement("{\"name\":\"Changed elsewhere\"}")
        registry.upsertEntry(entry, latest)
        assertEquals(latest, seleneJson.parseToJsonElement(api.read("active", registryName, entry).contents))
        assertNull(api.persist("active", registryName, entry).error)
        assertEquals(latest, seleneJson.parseToJsonElement(resources.loadAsString(path)))
        assertEquals(0, api.pendingChanges().count)
    }

    @Test
    fun `discard reloads the current file`() = fixture { api, resources, registry ->
        api.update("active", registryName, entry, "{\"name\":\"Applied\"}")
        val latest = "{\"name\":\"File changed elsewhere\"}"
        resources.saveAsString(path, latest)
        assertNull(api.discard("active", registryName, entry).error)
        assertEquals(seleneJson.parseToJsonElement(latest), registry.get(entry)!!.element)
        assertEquals(0, api.pendingChanges().count)
    }

    @Test
    fun `external mutations and reverts update dirty state`() = fixture { api, _, registry ->
        registry.upsertEntry(entry, seleneJson.parseToJsonElement("{\"name\":\"External mutation\"}"))
        assertEquals(listOf(path), api.pendingChanges().paths)
        registry.upsertEntry(entry, seleneJson.parseToJsonElement("{\"name\":\"Example\"}"))
        assertEquals(0, api.pendingChanges().count)
    }

    @Test
    fun `object key order is ignored and array order is preserved`() = fixture { api, resources, registry ->
        val original = """{"name":"Example","metadata":{"a":1,"b":2},"values":[1,2]}"""
        resources.saveAsString(path, original)
        registry.upsertEntry(entry, seleneJson.parseToJsonElement(original))
        registry.markPersisted(entry)
        registry.upsertEntry(entry, seleneJson.parseToJsonElement("""{"values":[1,2],"metadata":{"b":2,"a":1},"name":"Example"}"""))
        assertEquals(0, api.pendingChanges().count)
        registry.upsertEntry(entry, seleneJson.parseToJsonElement("""{"values":[2,1],"metadata":{"b":2,"a":1},"name":"Example"}"""))
        assertEquals(listOf(path), api.pendingChanges().paths)
    }

    @Test
    fun `runtime removals can be discarded or persisted`() = fixture { api, resources, registry ->
        registry.removeEntry(entry)
        assertEquals(listOf(path), api.pendingChanges().paths)
        assertNull(api.discard("active", registryName, entry).error)
        assertNotNull(registry.get(entry))
        registry.removeEntry(entry)
        assertNull(api.persist("active", registryName, entry).error)
        assertFalse(resources.fileExists(path))
        assertEquals(0, api.pendingChanges().count)
    }

    @Test
    fun `partial persistence keeps failed resources dirty`() = fixture { api, resources, _ ->
        resources.createAsString("active/common/data/test/items/missing", "blocked directory")
        api.update("active", registryName, Identifier("test", "a"), "{}")
        api.update("active", registryName, Identifier("test", "missing/z"), "{}")
        assertNotNull(api.persist().error)
        assertTrue(resources.fileExists("active/common/data/test/items/a.json"))
        assertEquals(listOf("active/common/data/test/items/missing/z.json"), api.pendingChanges().paths)
    }

    @Test
    fun `search uses metadata labels and exact lookup uses identifiers`() {
        val registry = CustomRegistry(seleneJson, CustomRegistryDefinition("items", "common"))
        registry.upsertEntry(Identifier.parse("test:example"), seleneJson.parseToJsonElement(
            """{"name":"Field label","metadata":{"name":"Example","visual":"test:preview"}}"""
        ))
        val provider = object : RegistryProvider {
            override fun getRegistries(): Map<Identifier, Registry<*>> = mapOf(Identifier("test", "items") to registry)
            override fun getRegistry(identifier: Identifier): Registry<*>? = registry.takeIf {
                identifier.toString() == "test:items"
            }
        }
        val bundles = BundleDatabase()
        val resources = ResourcesApi(bundles)
        val editor = EditorResources(resources, provider, seleneJson, bundles, Messages(bundles, LoggerFactory.getLogger(Messages::class.java)))
        val result = editor.searchRegistry(RegistrySearchRequest("test:items", "EXAM"))
        assertEquals(listOf(RegistryOption("test:example", "Example", "test:preview")), result.options)
        assertTrue(editor.searchRegistry(RegistrySearchRequest("test:items", "example", lookup = true)).options.isEmpty())
        assertEquals(result.options, editor.searchRegistry(RegistrySearchRequest("test:items", "TEST:EXAMPLE", lookup = true)).options)
    }
}
