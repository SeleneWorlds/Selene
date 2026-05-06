package com.seleneworlds.server.script

import com.seleneworlds.common.observable.ObservableMap
import com.seleneworlds.server.entities.EntityApi

interface ServerEntityScript {
    fun initialize(entity: EntityApi, data: ObservableMap): Boolean

    fun tick(entity: EntityApi, data: ObservableMap, delta: Float): Boolean
}
