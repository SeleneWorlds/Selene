package com.seleneworlds.client.script

import com.seleneworlds.client.entity.EntityApi
import com.seleneworlds.common.observable.ObservableMap

interface ClientEntityScript {
    fun initialize(entity: EntityApi, data: ObservableMap): Boolean

    fun tick(entity: EntityApi, data: ObservableMap, delta: Float): Boolean
}