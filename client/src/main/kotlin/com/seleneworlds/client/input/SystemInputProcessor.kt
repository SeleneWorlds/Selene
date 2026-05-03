package com.seleneworlds.client.input

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.InputAdapter
import com.seleneworlds.client.window.WindowManager

class SystemInputProcessor(
    private val windowManager: WindowManager
) : InputAdapter() {

    override fun keyDown(keycode: Int): Boolean {
        if (!isFullscreenToggle(keycode)) {
            return false
        }

        windowManager.toggleFullscreen()
        return true
    }

    private fun isFullscreenToggle(keycode: Int): Boolean {
        if (keycode != Input.Keys.ENTER) {
            return false
        }

        return Gdx.input.isKeyPressed(Input.Keys.ALT_LEFT) || Gdx.input.isKeyPressed(Input.Keys.ALT_RIGHT)
    }
}
