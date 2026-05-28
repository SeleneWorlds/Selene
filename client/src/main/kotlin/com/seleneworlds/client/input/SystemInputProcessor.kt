package com.seleneworlds.client.input

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.InputAdapter
import com.seleneworlds.client.window.WindowManager
import com.seleneworlds.common.bundles.BundleLifecycleManager

class SystemInputProcessor(
    private val windowManager: WindowManager,
    private val bundleLifecycleManager: BundleLifecycleManager
) : InputAdapter() {

    override fun keyDown(keycode: Int): Boolean {
        if (isFullscreenToggle(keycode)) {
            windowManager.toggleFullscreen()
            return true
        }

        if (isReloadKey(keycode)) {
            bundleLifecycleManager.reloadActiveBundles()
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
