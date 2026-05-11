package com.seleneworlds.client.input

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.InputAdapter
import com.seleneworlds.client.ClientReloadManager
import com.seleneworlds.client.window.WindowManager

class SystemInputProcessor(
    private val windowManager: WindowManager,
    private val clientReloadManager: ClientReloadManager
) : InputAdapter() {

    override fun keyDown(keycode: Int): Boolean {
        if (isFullscreenToggle(keycode)) {
            windowManager.toggleFullscreen()
            return true
        }

        if (isReloadKey(keycode)) {
            clientReloadManager.reloadRegistriesAndTextures()
            return true
        }

        return false
    }

    private fun isFullscreenToggle(keycode: Int): Boolean {
        if (keycode != Input.Keys.ENTER) {
            return false
        }

        return Gdx.input.isKeyPressed(Input.Keys.ALT_LEFT) || Gdx.input.isKeyPressed(Input.Keys.ALT_RIGHT)
    }

    private fun isReloadKey(keycode: Int): Boolean {
        return keycode == Input.Keys.F5
    }
}
