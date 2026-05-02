package com.seleneworlds.client.rendering.animator

import com.seleneworlds.client.entity.Entity

interface AnimatorController {
    val currentAnimation: ConfiguredAnimation?
    fun update(entity: Entity, delta: Float)
}