package com.seleneworlds.server.attributes

import com.seleneworlds.common.observable.Observer
import com.seleneworlds.server.attributes.filters.AttributeClampFilter.ConstantClampValue
import com.seleneworlds.server.attributes.filters.AttributeFilter
import com.seleneworlds.server.attributes.filters.IntAttributeClampFilter
import kotlin.test.Test
import kotlin.test.assertEquals

class AttributeTest {
    @Test
    fun `regeneration at the cap does not notify observers`() {
        val points = Attribute(Any(), "actionpoints", 21)
        points.addConstraint("clamp", IntAttributeClampFilter(ConstantClampValue(Int.MIN_VALUE), ConstantClampValue(21)))
        var notifications = 0
        points.subscribe(object : Observer<AttributeApi> {
            override fun notifyObserver(data: AttributeApi) { notifications++ }
        })
        repeat(100) { points.value = points.value + 1 }
        assertEquals(21, points.value)
        assertEquals(0, notifications)
        points.value = 1
        points.value = 2
        assertEquals(2, notifications)
        points.value = 100
        assertEquals(21, points.value)
        assertEquals(3, notifications)
        points.api.refresh()
        assertEquals(4, notifications)
    }

    @Test
    fun `notification compares the final result of all enabled constraints`() {
        val attribute = Attribute(Any(), "points", 5)
        val applied = mutableListOf<String>()
        fun constraint(name: String, transform: (Int) -> Int, active: Boolean = true) =
            object : AttributeFilter<Int> {
                override val enabled = active
                override fun apply(attribute: Attribute<Int>, value: Int): Int {
                    applied.add(name)
                    return transform(value)
                }
            }
        attribute.addConstraint("first", constraint("first", { it + 1 }))
        attribute.addConstraint("disabled", constraint("disabled", { error("Disabled") }, false))
        attribute.addConstraint("last", constraint("last", { 5 }))
        var notifications = 0
        attribute.subscribe(object : Observer<AttributeApi> {
            override fun notifyObserver(data: AttributeApi) { notifications++ }
        })
        attribute.value = 10
        assertEquals(listOf("first", "last"), applied)
        assertEquals(5, attribute.value)
        assertEquals(0, notifications)
    }
}
