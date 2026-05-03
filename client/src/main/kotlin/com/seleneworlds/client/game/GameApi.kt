package com.seleneworlds.client.game

import com.seleneworlds.client.window.WindowManager

class GameApi(
    private val windowManager: WindowManager
) {
    fun setWindowAspectRatio(width: Int, height: Int) {
        windowManager.setAspectRatio(width, height)
    }

    fun clearWindowAspectRatio() {
        windowManager.clearAspectRatio()
    }
}
