package com.seleneworlds.server.saves

import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import com.seleneworlds.common.observable.ObservableMap
import com.seleneworlds.common.serialization.seleneJson
import com.seleneworlds.server.maps.tree.MapTree
import java.io.File

class SaveManagerTest {

    @Test
    fun `saves and loads observable maps as json`() {
        val saveManager = SaveManager(NoopMapTreeFormat, seleneJson)
        val tempDir = createTempDirectory("save-manager-test").toFile()
        val saveFile = tempDir.resolve("save-test.json")
        val savable = ObservableMap(
            mutableMapOf(
                "counter" to 5,
                "nested" to ObservableMap(mutableMapOf("enabled" to true)),
                "list" to listOf("a", 1, false)
            )
        )

        saveManager.save(saveFile, savable)

        assertTrue(saveFile.isFile)

        val loaded = saveManager.load(saveFile)
        val loadedMap = assertIs<ObservableMap>(loaded)
        assertEquals(5, loadedMap["counter"])
        assertEquals(listOf("a", 1, false), loadedMap["list"])
        assertEquals(mapOf("enabled" to true), loadedMap["nested"])
    }

    private object NoopMapTreeFormat : MapTreeFormat {
        override fun load(file: File): MapTree {
            error("Not used in this test")
        }

        override fun saveFullyInline(file: java.io.File, mapTree: MapTree) = Unit
    }
}
