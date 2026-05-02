package com.seleneworlds.client.entity.component.rendering

class ReloadableVisualComponentApi(val component: ReloadableVisualComponent) {

    fun getVisual() = component.visual.api

    fun getRed(): Float = component.red

    fun setRed(value: Float) {
        component.red = value
    }

    fun getGreen(): Float = component.green

    fun setGreen(value: Float) {
        component.green = value
    }

    fun getBlue(): Float = component.blue

    fun setBlue(value: Float) {
        component.blue = value
    }

    fun getAlpha(): Float = component.alpha

    fun setAlpha(value: Float) {
        component.alpha = value
    }
}
