package com.seleneworlds.common.event

import com.seleneworlds.common.data.Identifier

object EventPhases {
    val LOWEST = Identifier.withDefaultNamespace("lowest")
    val LOW = Identifier.withDefaultNamespace("low")
    val DEFAULT = Identifier.withDefaultNamespace("default")
    val HIGH = Identifier.withDefaultNamespace("high")
    val HIGHEST = Identifier.withDefaultNamespace("highest")
}
