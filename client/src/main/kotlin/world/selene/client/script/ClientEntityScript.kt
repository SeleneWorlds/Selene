package world.selene.client.script

import world.selene.client.entity.EntityApi
import world.selene.common.observable.ObservableMap

interface ClientEntityScript {
    fun initialize(entity: EntityApi, data: ObservableMap): Boolean

    fun tick(entity: EntityApi, data: ObservableMap, delta: Float): Boolean
}