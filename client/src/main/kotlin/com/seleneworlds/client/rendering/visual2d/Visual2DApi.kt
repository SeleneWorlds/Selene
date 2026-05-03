package com.seleneworlds.client.rendering.visual2d

interface Visual2DApi {
    val delegate: Visual2D
    fun getMetadata(key: String): Any?
}