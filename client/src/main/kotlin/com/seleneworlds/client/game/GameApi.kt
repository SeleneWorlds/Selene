package com.seleneworlds.client.game

import com.seleneworlds.client.window.WindowManager
import com.seleneworlds.client.window.ScalingStrategy

class GameApi(
    private val windowManager: WindowManager
) {
    fun setWindowAspectRatio(width: Int, height: Int) {
        windowManager.setAspectRatio(width, height)
    }

    fun clearWindowAspectRatio() {
        windowManager.clearAspectRatio()
    }

    fun setWindowScaling(strategy: ScalingStrategy, baseWidth: Int? = null, baseHeight: Int? = null) {
        windowManager.setScalingStrategy(strategy, baseWidth, baseHeight)
    }
}
