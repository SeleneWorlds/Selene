package com.seleneworlds.common.data.custom

import com.seleneworlds.common.bundles.BundleDatabase
import com.seleneworlds.common.data.Identifier
import com.seleneworlds.common.data.Registry
import com.seleneworlds.common.data.RegistryReloadListener
import com.seleneworlds.common.data.RegistryProvider
import com.seleneworlds.common.data.RegistriesApi
import com.seleneworlds.common.data.RegistriesLuaApi
import com.seleneworlds.common.lua.LuaManager
import com.seleneworlds.common.lua.libraries.LuaPackageModule
import com.seleneworlds.common.lua.util.newTable
import com.seleneworlds.common.serialization.seleneJson
import kotlinx.serialization.json.*
import kotlin.test.*

class RegistryValueReferencesTest {
    private val manager = CustomRegistries(seleneJson).also {
        for (name in listOf("shared", "fields", "behaviors")) {
            it.upsertEntry(id(name), json("""{"name":"$name","platform":"server"}"""))
        }
        it.loadCustomRegistries(BundleDatabase(), "server")
    }
    private fun id(name: String) = Identifier("test", name)
    private fun json(value: String) = seleneJson.parseToJsonElement(value)
    private fun registry(name: String) = manager.getCustomRegistry(id(name)) as CustomRegistry
    private fun put(registry: String, name: String, value: String): CustomRegistryObject {
        val target = registry(registry)
        target.upsertEntry(id(name), json(value))
        return target.get(id(name))!!
    }
    private fun ref(registry: String, entry: String, path: String = "") =
        """{"${'$'}ref":{"registry":"test:$registry","entry":"test:$entry","path":"$path"}}"""

    @Test
    fun resolvesNestedCoordinatesAndWholeBehaviorEntriesWithoutChangingRawJson() {
        put("shared", "room", """{"positions":{"door":{"x":-74,"y":-68,"z":0}},"null":null}""")
        put("behaviors", "cleanup", """{"event":"logout","actions":[{"data":{"position":${ref("shared", "room", "/positions/door")}}}]}""")
        val raw = """{"coordinate":${ref("shared", "room", "/positions/door")},"behaviors":[${ref("behaviors", "cleanup")},{"event":"inline"}],"literal":false,"nullable":${ref("shared", "room", "/null")}}"""
        val entry = put("fields", "door", raw)
        assertEquals(json("""{"x":-74,"y":-68,"z":0}"""), entry.getField("coordinate"))
        val behaviors = entry.getField("behaviors")!!.jsonArray
        assertEquals(entry.getField("coordinate"), behaviors[0].jsonObject["actions"]!!.jsonArray[0].jsonObject["data"]!!.jsonObject["position"])
        assertEquals("inline", behaviors[1].jsonObject["event"]!!.jsonPrimitive.content)
        assertEquals(JsonPrimitive(false), entry.getField("literal"))
        assertEquals(JsonNull, entry.getField("nullable"))
        assertNull(entry.getField("absent"))
        assertEquals(json(raw), registry("fields").getEntryElement(id("door")))
    }

    @Test
    fun supportsJsonPointerEscapesArraysAndIntermediateReferences() {
        put("shared", "room", """{"a/b":{"~key":[42]},"alias":${ref("shared", "room", "/a~1b")}}""")
        val entry = put("fields", "door", """{"value":${ref("shared", "room", "/alias/~0key/0")}}""")
        assertEquals(JsonPrimitive(42), entry.getField("value"))
        // Selecting a leaf does not attempt to resolve unrelated siblings.
        put("shared", "room", """{"a/b":{"~key":[43]},"broken":${ref("shared", "missing")},"alias":${ref("shared", "room", "/a~1b")}}""")
        assertEquals(JsonPrimitive(43), entry.getField("value"))
    }

    @Test
    fun rejectsMissingTargetsMalformedPointersAndReferenceSiblingsWithSourceContext() {
        put("shared", "room", """{"list":[1]}""")
        for (reference in listOf(
            ref("unknown", "room"), ref("shared", "missing"), ref("shared", "room", "/missing"),
            ref("shared", "room", "/list/01"), ref("shared", "room", "/list/1"),
            ref("shared", "room", "/bad~2escape"), ref("shared", "room", "list"),
            """{"${'$'}ref":"test:room"}""", """{"${'$'}ref":{"registry":3}}""",
            ref("shared", "room").dropLast(1) + ",\"extra\":true}"
        )) {
            val entry = put("fields", "door", """{"value":$reference}""")
            val failure = assertFailsWith<IllegalArgumentException> { entry.getField("value") }
            assertContains(failure.message!!, "test:fields/test:door/value")
        }
    }

    @Test
    fun reportsCrossRegistryCyclesAndAllowsRepeatedIndependentReferences() {
        put("shared", "a", """{"value":${ref("fields", "b", "/value")}}""")
        val b = put("fields", "b", """{"value":${ref("shared", "a", "/value")}}""")
        val error = assertFailsWith<IllegalArgumentException> { b.getField("value") }
        assertContains(error.message!!, "Cyclic")
        assertContains(error.message!!, "test:a")
        assertContains(error.message!!, "test:b")
        put("shared", "a", """{"value":1}""")
        val entry = put("fields", "twice", """{"values":[${ref("shared", "a", "/value")},${ref("shared", "a", "/value")}]}""")
        assertEquals(json("[1,1]"), entry.getField("values"))
    }

    @Test
    fun notifiesTransitiveDependentsOnceWithoutDirtyingThemAndTracksChangedReferences() {
        put("shared", "room", """{"x":1}""")
        put("shared", "other", """{"x":9}""")
        put("behaviors", "alias", """{"value":${ref("shared", "room", "/x")}}""")
        val entry = put("fields", "door", """{"coordinate":${ref("behaviors", "alias", "/value")},"duplicate":${ref("shared", "room", "/x")}}""")
        registry("fields").upsertEntry(id("door"), entry.element, sourcePath = "test/server/data/test/fields/door.json")
        registry("fields").markPersisted(id("door"))
        var notifications = 0
        registry("fields").addReloadListener(object : RegistryReloadListener<CustomRegistryObject> {
            override fun onEntryChanged(registry: Registry<CustomRegistryObject>, identifier: Identifier, oldData: CustomRegistryObject, newData: CustomRegistryObject) {
                notifications++
            }
        })
        val oldCacheKey = registry("fields").cacheKey
        put("shared", "room", """{"x":2}""")
        assertEquals(1, notifications)
        assertTrue(registry("fields").cacheKey > oldCacheKey)
        assertEquals(JsonPrimitive(2), entry.getField("coordinate"))
        assertFalse(registry("fields").isDirty(id("door")))
        registry("shared").removeEntry(id("room"))
        assertEquals(2, notifications)
        assertFailsWith<IllegalArgumentException> { entry.getField("coordinate") }
        put("shared", "room", """{"x":3}""")
        assertEquals(3, notifications)
        put("behaviors", "alias", """{"value":${ref("shared", "other", "/x")}}""")
        assertEquals(JsonPrimitive(9), entry.getField("coordinate"))
        put("shared", "other", """{"x":10}""")
        assertEquals(5, notifications)
        assertEquals(JsonPrimitive(10), entry.getField("coordinate"))
    }

    @Test
    fun validatesReferencedCoordinatesAreasAndNamedBehaviorSchemas() {
        val registries = mutableMapOf<Identifier, CustomRegistry>()
        val references = RegistryValueReferences { registries }
        val shared = CustomRegistry(seleneJson, CustomRegistryDefinition("shared", "server"), references)
        registries[id("shared")] = shared
        val schemas = CustomRegistry(seleneJson, CustomRegistryDefinition("schemas", "common"), references)
        registries[Identifier("selene", "schemas")] = schemas
        schemas.upsertEntry(id("behavior"), json("""{"definition":{"type":"object","properties":{"actions":{"type":"array","valueType":"any"}}}}"""))
        val fields = CustomRegistry(seleneJson, CustomRegistryDefinition("fields", "server", schema = json("""{"coordinate":"coordinate","area":"area","behavior":"test:behavior"}""").jsonObject), references)
        registries[id("fields")] = fields
        shared.upsertEntry(id("room"), json("""{"coordinate":{"x":1,"y":2,"z":0},"area":{"include":[{"type":"rectangle","x":1,"y":2,"z":0,"width":2,"height":2}],"exclude":[]},"behavior":{"actions":[]}}"""))
        fields.upsertEntry(id("door"), json("""{"coordinate":${ref("shared", "room", "/coordinate")},"area":${ref("shared", "room", "/area")},"behavior":${ref("shared", "room", "/behavior")}}"""))
        val entry = fields.get(id("door"))!!
        assertNotNull(entry.getField("coordinate"))
        assertNotNull(entry.getField("area"))
        assertNotNull(entry.getField("behavior"))
        shared.upsertEntry(id("room"), json("""{"coordinate":{"x":"bad","y":2,"z":0},"area":{"include":42,"exclude":[]},"behavior":{"actions":"bad"}}"""))
        for (key in listOf("coordinate", "area", "behavior")) {
            assertFailsWith<IllegalArgumentException> { entry.getField(key) }
        }
    }

    @Test
    fun fullTargetReloadNotifiesDependentsEvenWhenTargetsDisappear() {
        put("shared", "room", """{"x":1}""")
        val entry = put("fields", "door", """{"value":${ref("shared", "room", "/x")}}""")
        var notifications = 0
        registry("fields").subscribe(registry("fields").getReference(id("door"))) { notifications++ }
        assertEquals(1, notifications)
        registry("shared").load(BundleDatabase())
        assertEquals(2, notifications)
        assertFailsWith<IllegalArgumentException> { entry.getField("value") }
    }

    @Test
    fun changesMadeByDependentListenersAlsoInvalidateTheirOwnDependents() {
        put("shared", "a", """{"value":1}""")
        put("shared", "b", """{"value":2}""")
        put("fields", "a", """{"value":${ref("shared", "a", "/value")}}""")
        val b = put("fields", "b", """{"value":${ref("shared", "b", "/value")}}""")
        val notifications = mutableListOf<Identifier>()
        registry("fields").addReloadListener(object : RegistryReloadListener<CustomRegistryObject> {
            override fun onEntryChanged(registry: Registry<CustomRegistryObject>, identifier: Identifier, oldData: CustomRegistryObject, newData: CustomRegistryObject) {
                notifications.add(identifier)
                if (identifier == id("a")) put("shared", "b", """{"value":3}""")
            }
        })
        put("shared", "a", """{"value":4}""")
        assertEquals(listOf(id("a"), id("b")), notifications)
        assertEquals(JsonPrimitive(3), b.getField("value"))
    }

    @Test
    fun luaConsumersReceiveResolvedTablesAndRefreshOnReferencedChanges() {
        put("shared", "room", """{"door":{"x":1,"y":2,"z":0}}""")
        put("fields", "door", """{"coordinate":${ref("shared", "room", "/door")}}""")
        val provider = object : RegistryProvider {
            override fun getRegistries(): Map<Identifier, Registry<*>> = manager.getCustomRegistryEntries()
            override fun getRegistry(identifier: Identifier): Registry<*>? = manager.getCustomRegistry(identifier)
        }
        val luaManager = LuaManager(LuaPackageModule())
        val lua = luaManager.lua
        try {
            val api = RegistriesLuaApi(RegistriesApi(provider))
            lua.push(lua.newTable { api.register(this) })
            lua.setGlobal("Registries")
            fun run(source: String) {
                lua.load(LuaManager.loadBuffer(source), "registry_value_references")
                lua.pCall(0, 0)
            }
            run("""
                annotations = {}
                reloads = 0
                function refresh()
                    annotations = {}
                    for _, field in pairs(Registries.findAll("test:fields")) do
                        local coordinate = field:getField("coordinate")
                        annotations[coordinate.x] = true
                    end
                    reloads = reloads + 1
                end
                refresh()
                Registries.entryChanged("test:fields"):connect(refresh)
                assert(annotations[1])
            """.trimIndent())
            put("shared", "room", """{"door":{"x":7,"y":2,"z":0}}""")
            run("assert(annotations[7] and not annotations[1]); assert(reloads == 2)")
            assertEquals(0, lua.top)
        } finally {
            lua.close()
        }
    }
}
