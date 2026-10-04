package com.seleneworlds.common.serialization

import com.seleneworlds.common.entities.ComponentConfiguration
import com.seleneworlds.common.entities.DraggableComponentConfiguration
import kotlin.test.Test
import kotlin.test.assertEquals

class SeleneJsonDefaultsTest {
    @Test
    fun `serialized values include properties equal to their defaults`() {
        val component: ComponentConfiguration = DraggableComponentConfiguration()

        assertEquals(
            "{\"type\":\"draggable\",\"enabled\":true}",
            seleneJson.encodeToString(component)
        )
    }
}
