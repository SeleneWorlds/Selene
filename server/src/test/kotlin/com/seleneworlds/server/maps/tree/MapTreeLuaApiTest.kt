package com.seleneworlds.server.maps.tree

import com.seleneworlds.common.data.custom.CustomRegistries
import com.seleneworlds.common.entities.EntityRegistry
import com.seleneworlds.common.entities.component.ComponentRegistry
import com.seleneworlds.common.grid.Coordinate
import com.seleneworlds.common.grid.GridRegistry
import com.seleneworlds.common.lua.LuaManager
import com.seleneworlds.common.lua.libraries.LuaPackageModule
import com.seleneworlds.common.sounds.SoundRegistry
import com.seleneworlds.common.tiles.TileRegistry
import com.seleneworlds.common.tiles.transitions.TransitionRegistry
import com.seleneworlds.server.data.Registries
import com.seleneworlds.server.maps.layers.DenseMapLayer
import com.seleneworlds.server.maps.layers.MapLayerApi
import com.seleneworlds.server.maps.layers.MapLayerLuaApi
import com.seleneworlds.server.saves.SavesApi
import com.seleneworlds.server.saves.SavesLuaApi
import com.seleneworlds.server.saves.SaveManager
import com.seleneworlds.server.saves.MapTreeFormatBinaryV1
import com.seleneworlds.server.config.ServerConfig
import com.seleneworlds.common.serialization.seleneJson
import com.seleneworlds.common.lua.util.newTable
import kotlin.io.path.createTempDirectory
import kotlinx.serialization.json.Json
import party.iroiro.luajava.Lua
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MapTreeLuaApiTest {
    @Test
    fun `lua can save a layer directly and restore it with annotation removals`() {
        val registries = Registries(
            TileRegistry(Json), TransitionRegistry(Json), EntityRegistry(Json), ComponentRegistry(Json),
            GridRegistry(Json), SoundRegistry(Json), CustomRegistries(Json)
        )
        val directory = createTempDirectory("layer-lua-save").toFile()
        val source = MapTree(registries)
        val target = MapTree(registries)
        val coordinate = Coordinate(1, 2, 0)
        target.annotateTile(coordinate, "removed", emptyMap())
        val manager = LuaManager(LuaPackageModule())
        try {
            manager.defineMetatable(MapTreeApi::class, MapTreeLuaApi.luaMeta)
            manager.defineMetatable(MapLayerApi::class, MapLayerLuaApi.luaMeta)
            val saves = SavesLuaApi(SavesApi(SaveManager(MapTreeFormatBinaryV1(registries), seleneJson),
                ServerConfig(savePath = directory.absolutePath)))
            manager.lua.push(manager.lua.newTable { saves.register(this) })
            manager.lua.setGlobal("Saves")
            manager.lua.push(source.api, Lua.Conversion.NONE)
            manager.lua.setGlobal("source")
            manager.lua.push(target.api, Lua.Conversion.NONE)
            manager.lua.setGlobal("target")
            manager.lua.load(LuaManager.loadBuffer(
                """
                source:annotateTile(1, 2, 0, "removed", nil, "admin")
                source:annotateTile(1, 2, 0, "added", {x = 3}, "admin")
                local layer = source:getLayer("admin")
                layerName = layer:getName()
                Saves.save(layer, "admin.selenemap")
                local loaded = Saves.loadMapLayer("admin.selenemap")
                loadedLayerName = loaded:getName()
                target:addLayer(loaded)
                """.trimIndent()
            ), "restore_overlay")
            manager.lua.pCall(0, 0)
            manager.lua.getGlobal("layerName")
            assertEquals("admin", manager.lua.toString(-1))
            manager.lua.pop(1)
            manager.lua.getGlobal("loadedLayerName")
            assertEquals("admin", manager.lua.toString(-1))
            manager.lua.pop(1)
            assertEquals(mapOf(coordinate to mapOf("added" to mapOf("x" to 3))),
                target.getAllAnnotations(com.seleneworlds.server.cameras.viewer.DefaultViewer))
        } finally {
            manager.lua.close()
            directory.deleteRecursively()
        }
    }

    @Test
    fun `lua can remove a triggerfield annotation with nil without removing other annotations`() {
        val registries = Registries(
            TileRegistry(Json), TransitionRegistry(Json), EntityRegistry(Json), ComponentRegistry(Json),
            GridRegistry(Json), SoundRegistry(Json), CustomRegistries(Json)
        )
        val layer = DenseMapLayer("base", registries)
        val map = MapTree(registries).apply { baseLayer = layer }
        val manager = LuaManager(LuaPackageModule())
        try {
            manager.defineMetatable(MapTreeApi::class, MapTreeLuaApi.luaMeta)
            manager.lua.push(map.api, Lua.Conversion.NONE)
            manager.lua.setGlobal("map")
            manager.lua.load(LuaManager.loadBuffer(
                """
                map:annotateTile(1, 2, 0, "example:warp", {x = 3})
                map:annotateTile(1, 2, 0, "example:triggerfield", {script = "old"})
                map:annotateTile(1, 2, 0, "example:triggerfield", nil)
                """.trimIndent()
            ), "remove_triggerfield")
            manager.lua.pCall(0, 0)
            val annotations = layer.getAnnotations(Coordinate(1, 2, 0))
            assertTrue("example:triggerfield" !in annotations)
            assertEquals(3, annotations["example:warp"]?.get("x"))
        } finally {
            manager.lua.close()
        }
    }
}
