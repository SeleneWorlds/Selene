package com.seleneworlds.client.window

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Graphics
import org.lwjgl.glfw.GLFW

class WindowManager {
    private var aspectRatio: Pair<Int, Int>? = null
    private var wasFullscreen = false
    private var windowedWidth = 1024
    private var windowedHeight = 768

    fun setAspectRatio(width: Int, height: Int) {
        require(width > 0) { "Aspect ratio width must be positive" }
        require(height > 0) { "Aspect ratio height must be positive" }

        aspectRatio = width to height
        GLFW.glfwSetWindowAspectRatio(getWindowHandle(), width, height)
    }

    fun clearAspectRatio() {
        aspectRatio = null
        GLFW.glfwSetWindowAspectRatio(getWindowHandle(), GLFW.GLFW_DONT_CARE, GLFW.GLFW_DONT_CARE)
    }

    fun toggleFullscreen() {
        val graphics = Gdx.graphics
        if (wasFullscreen) {
            graphics.setWindowedMode(windowedWidth, windowedHeight)
            reapplyWindowedPolicy()
            wasFullscreen = false
            return
        }

        windowedWidth = graphics.width
        windowedHeight = graphics.height
        graphics.displayMode?.let { graphics.setFullscreenMode(it) }
        wasFullscreen = true
    }

    private fun reapplyWindowedPolicy() {
        val currentAspectRatio = aspectRatio
        if (currentAspectRatio != null) {
            GLFW.glfwSetWindowAspectRatio(getWindowHandle(), currentAspectRatio.first, currentAspectRatio.second)
        } else {
            GLFW.glfwSetWindowAspectRatio(getWindowHandle(), GLFW.GLFW_DONT_CARE, GLFW.GLFW_DONT_CARE)
        }
    }

    private fun getWindowHandle(): Long {
        val graphics = Gdx.graphics as? Lwjgl3Graphics
            ?: throw IllegalStateException("Window aspect ratio is only supported on LWJGL3")
        return graphics.window.windowHandle
    }
}
