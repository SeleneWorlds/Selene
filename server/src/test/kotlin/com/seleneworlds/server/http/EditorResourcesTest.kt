package com.seleneworlds.server.http

import com.seleneworlds.common.bundles.*
import com.seleneworlds.common.data.*
import com.seleneworlds.common.data.custom.*
import com.seleneworlds.common.serialization.seleneJson
import kotlinx.serialization.json.*
import java.nio.file.Files
import kotlin.test.*

class EditorResourcesTest {
    private val registryName = Identifier("test", "items")
    private val entry = Identifier("test", "example")
    private val path = "active/common/data/test/items/example.json"

    private fun fixture(
        registryIdentifier: Identifier = registryName,
        includeEmptyBundle: Boolean = false,
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
            val registry = CustomRegistry(seleneJson, CustomRegistryDefinition("items", "common"))
            registry.load(database)
            val provider = object : RegistryProvider {
                override fun getRegistries(): Map<Identifier, Registry<*>> = mapOf(registryIdentifier to registry)
                override fun getRegistry(identifier: Identifier): Registry<*>? = registry.takeIf { identifier == registryIdentifier }
            }
            val resources = ResourcesApi(database)
            val editor = EditorResources(resources, provider, seleneJson, database)
            test(editor, resources, registry)
        } finally {
            root.toFile().deleteRecursively()
        }
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
        val editor = EditorResources(resources, provider, seleneJson, bundles)
        val result = editor.searchRegistry(RegistrySearchRequest("test:items", "EXAM"))
        assertEquals(listOf(RegistryOption("test:example", "Example", "test:preview")), result.options)
        assertTrue(editor.searchRegistry(RegistrySearchRequest("test:items", "example", lookup = true)).options.isEmpty())
        assertEquals(result.options, editor.searchRegistry(RegistrySearchRequest("test:items", "TEST:EXAMPLE", lookup = true)).options)
    }
}
