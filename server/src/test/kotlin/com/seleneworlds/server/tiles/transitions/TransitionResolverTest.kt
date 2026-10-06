package com.seleneworlds.server.tiles.transitions

import com.seleneworlds.common.data.Identifier
import com.seleneworlds.common.data.custom.CustomRegistries
import com.seleneworlds.common.entities.EntityRegistry
import com.seleneworlds.common.entities.component.ComponentRegistry
import com.seleneworlds.common.grid.ChunkWindow
import com.seleneworlds.common.grid.GridRegistry
import com.seleneworlds.common.sounds.SoundRegistry
import com.seleneworlds.common.tiles.TileRegistry
import com.seleneworlds.common.tiles.transitions.TransitionRegistry
import com.seleneworlds.server.data.Registries
import com.seleneworlds.server.sync.ScopedChunkView
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put
import kotlin.test.Test
import kotlin.test.assertTrue

class TransitionResolverTest {
    private val json = Json

    @Test
    fun `does not place transitions on an empty center tile`() {
        val tiles = TileRegistry(json).apply {
            upsertEntry(Identifier("test", "source"), tileJson(), id = 7)
            upsertEntry(Identifier("test", "transition"), tileJson(), id = 8)
        }
        val transitions = TransitionRegistry(json).apply {
            upsertEntry(
                Identifier("test", "source"),
                json.parseToJsonElement(
                    """{
                        "priority": 1,
                        "transitions": [{
                            "tile": "test:transition",
                            "neighbours": ["010", "000", "000"]
                        }]
                    }"""
                ).jsonObject
            )
        }
        val registries = Registries(
            tiles,
            transitions,
            EntityRegistry(json),
            ComponentRegistry(json),
            GridRegistry(json),
            SoundRegistry(json),
            CustomRegistries(json)
        )
        val view = ScopedChunkView(ChunkWindow(0, 0, 1, 1, 1))
        view.baseTiles[view.padding] = 7 // North of the empty center cell.

        TransitionResolver(registries).applyTransitions(view)

        assertTrue(view.additionalTiles.isEmpty)
    }

    private fun tileJson() = buildJsonObject {
        put("visual", "test:visual")
    }
}
