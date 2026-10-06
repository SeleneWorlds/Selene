package com.seleneworlds.common.serialization

import com.seleneworlds.common.entities.ComponentConfiguration
import com.seleneworlds.common.entities.DraggableComponentConfiguration
import com.seleneworlds.common.entities.IgnoresElevationComponentConfiguration
import com.seleneworlds.common.entities.GravityComponentConfiguration
import com.seleneworlds.common.entities.PassableAboveComponentConfiguration
import kotlin.test.Test
import kotlin.test.assertEquals

class SeleneJsonDefaultsTest {
    @Test
    fun `gravity component serializes with its type`() {
        val component: ComponentConfiguration = GravityComponentConfiguration()

        assertEquals("{\"type\":\"gravity\"}", seleneJson.encodeToString(component))
    }

    @Test
    fun `serialized values include properties equal to their defaults`() {
        val component: ComponentConfiguration = DraggableComponentConfiguration()

        assertEquals(
            "{\"type\":\"draggable\",\"enabled\":true}",
            seleneJson.encodeToString(component)
        )
    }

    @Test
    fun `marker components serialize with their type`() {
        val component: ComponentConfiguration = IgnoresElevationComponentConfiguration()

        assertEquals(
            "{\"type\":\"ignores_elevation\"}",
            seleneJson.encodeToString(component)
        )
    }

    @Test
    fun `passable above component serializes with its default`() {
        val component: ComponentConfiguration = PassableAboveComponentConfiguration()

        assertEquals(
            "{\"type\":\"passable_above\",\"enabled\":true}",
            seleneJson.encodeToString(component)
        )
    }
}
