package com.seleneworlds.client.input

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.InputAdapter

class SystemInputProcessor : InputAdapter() {

    private var wasFullscreen = false
    private var windowedWidth = 1024
    private var windowedHeight = 768

    override fun keyDown(keycode: Int): Boolean {
        if (!isFullscreenToggle(keycode)) {
            return false
        }

        toggleFullscreen()
        return true
    }

    private fun isFullscreenToggle(keycode: Int): Boolean {
        if (keycode != Input.Keys.ENTER) {
            return false
        }

        return Gdx.input.isKeyPressed(Input.Keys.ALT_LEFT) || Gdx.input.isKeyPressed(Input.Keys.ALT_RIGHT)
    }

    private fun toggleFullscreen() {
        val graphics = Gdx.graphics
        if (wasFullscreen) {
            graphics.setWindowedMode(windowedWidth, windowedHeight)
            wasFullscreen = false
            return
        }

        windowedWidth = graphics.width
        windowedHeight = graphics.height
        graphics.displayMode?.let { graphics.setFullscreenMode(it) }
        wasFullscreen = true
    }
}
