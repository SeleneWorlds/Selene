package com.seleneworlds.server.attributes.filters

import com.seleneworlds.server.attributes.Attribute

interface AttributeFilter<T : Any?> {
    val enabled: Boolean
    fun apply(attribute: Attribute<T>, value: T): T
}