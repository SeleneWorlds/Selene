package com.seleneworlds.server.attributes

import com.seleneworlds.server.attributes.filters.AttributeClampFilter
import com.seleneworlds.server.attributes.filters.AttributeMathOpFilter
import com.seleneworlds.server.attributes.filters.IntAttributeClampFilter
import com.seleneworlds.server.attributes.filters.IntAttributeMathOpFilter

class AttributesApi {

    fun clampFilter(min: Any, max: Any): IntAttributeClampFilter {
        return IntAttributeClampFilter(toClampValue(min), toClampValue(max))
    }

    fun mathOpFilter(value: Any, operator: String): IntAttributeMathOpFilter {
        return IntAttributeMathOpFilter(toMathOpValue(value), AttributeMathOpFilter.MathOp.fromSign(operator))
    }

    @Suppress("UNCHECKED_CAST")
    private fun toClampValue(value: Any): AttributeClampFilter.ClampValue<Int> {
        return when (value) {
            is Number -> AttributeClampFilter.ConstantClampValue(value.toInt())
            is AttributeApi -> AttributeClampFilter.AttributeClampValue(value.attribute as Attribute<Int>)
            else -> throw IllegalArgumentException("Expected number or Attribute")
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun toMathOpValue(value: Any): AttributeMathOpFilter.MathOpValue<Int> {
        return when (value) {
            is Number -> AttributeMathOpFilter.ConstantMathOpValue(value.toInt())
            is AttributeApi -> AttributeMathOpFilter.AttributeMathOpValue(value.attribute as Attribute<Int>)
            else -> throw IllegalArgumentException("Expected number or Attribute")
        }
    }
}
