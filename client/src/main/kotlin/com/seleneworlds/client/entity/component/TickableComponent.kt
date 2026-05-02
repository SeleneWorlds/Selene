package com.seleneworlds.client.entity.component

import com.seleneworlds.client.entity.Entity

interface TickableComponent {
    fun update(entity: Entity, delta: Float)
}