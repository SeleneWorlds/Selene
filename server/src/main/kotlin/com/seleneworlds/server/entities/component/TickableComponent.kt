package com.seleneworlds.server.entities.component

import com.seleneworlds.server.entities.Entity

interface TickableComponent {
    fun update(entity: Entity, delta: Float)
}
