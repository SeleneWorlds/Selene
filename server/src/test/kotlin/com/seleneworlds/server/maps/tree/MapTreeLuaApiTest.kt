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
import kotlinx.serialization.json.Json
import party.iroiro.luajava.Lua
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MapTreeLuaApiTest {
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
                map:annotateTile(1, 2, 0, "illarion:warp", {x = 3})
                map:annotateTile(1, 2, 0, "illarion:triggerfield", {script = "old"})
                map:annotateTile(1, 2, 0, "illarion:triggerfield", nil)
                """.trimIndent()
            ), "remove_triggerfield")
            manager.lua.pCall(0, 0)
            val annotations = layer.getAnnotations(Coordinate(1, 2, 0))
            assertTrue("illarion:triggerfield" !in annotations)
            assertEquals(3, annotations["illarion:warp"]?.get("x"))
        } finally {
            manager.lua.close()
        }
    }
}
