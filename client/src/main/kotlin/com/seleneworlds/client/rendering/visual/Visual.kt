package com.seleneworlds.client.rendering.visual

import com.seleneworlds.common.threading.Awaitable

interface Visual {
    fun initialize(): Awaitable<Void?> = Awaitable.completed(null)
    fun update(delta: Float)
}
