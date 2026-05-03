package com.seleneworlds.client.window

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Graphics
import org.lwjgl.glfw.GLFW
import kotlin.math.roundToInt

class WindowManager {
    private var aspectRatio: Pair<Int, Int>? = null
    private var scalingStrategy = ScalingStrategy.NONE
    private var baseWidth: Int? = null
    private var baseHeight: Int? = null
    private var wasFullscreen = false
    private var windowedWidth = 1024
    private var windowedHeight = 768
    private var windowWidth = 1024
    private var windowHeight = 768
    private var layoutVersion = 0L

    var viewport = WindowViewport(
        logicalWidth = windowWidth,
        logicalHeight = windowHeight,
        screenX = 0,
        screenY = 0,
        screenWidth = windowWidth,
        screenHeight = windowHeight
    )
        private set

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

    fun setScalingStrategy(strategy: ScalingStrategy, baseWidth: Int? = null, baseHeight: Int? = null) {
        when (strategy) {
            ScalingStrategy.NONE -> {
                this.baseWidth = null
                this.baseHeight = null
            }

            ScalingStrategy.UNIFORM -> {
                require(baseWidth != null && baseWidth > 0) { "Uniform scaling base width must be positive" }
                require(baseHeight != null && baseHeight > 0) { "Uniform scaling base height must be positive" }
                this.baseWidth = baseWidth
                this.baseHeight = baseHeight
            }
        }

        scalingStrategy = strategy
        recomputeViewport()
    }

    fun getLayoutVersion(): Long = layoutVersion

    fun updateWindowSize(width: Int, height: Int) {
        require(width > 0) { "Window width must be positive" }
        require(height > 0) { "Window height must be positive" }

        if (windowWidth == width && windowHeight == height) {
            return
        }

        windowWidth = width
        windowHeight = height
        recomputeViewport()
    }

    fun toggleFullscreen() {
        val graphics = Gdx.graphics
        if (wasFullscreen) {
            graphics.setWindowedMode(windowedWidth, windowedHeight)
            updateWindowSize(graphics.width, graphics.height)
            reapplyWindowedPolicy()
            wasFullscreen = false
            return
        }

        windowedWidth = windowWidth
        windowedHeight = windowHeight
        graphics.displayMode?.let { graphics.setFullscreenMode(it) }
        updateWindowSize(graphics.width, graphics.height)
        wasFullscreen = true
    }

    private fun recomputeViewport() {
        viewport = when (scalingStrategy) {
            ScalingStrategy.NONE -> WindowViewport(
                logicalWidth = windowWidth,
                logicalHeight = windowHeight,
                screenX = 0,
                screenY = 0,
                screenWidth = windowWidth,
                screenHeight = windowHeight
            )

            ScalingStrategy.UNIFORM -> {
                val logicalWidth = baseWidth ?: error("Uniform scaling base width is not set")
                val logicalHeight = baseHeight ?: error("Uniform scaling base height is not set")
                val scale = minOf(
                    windowWidth.toFloat() / logicalWidth.toFloat(),
                    windowHeight.toFloat() / logicalHeight.toFloat()
                )
                val screenWidth = (logicalWidth * scale).roundToInt().coerceAtLeast(1)
                val screenHeight = (logicalHeight * scale).roundToInt().coerceAtLeast(1)
                WindowViewport(
                    logicalWidth = logicalWidth,
                    logicalHeight = logicalHeight,
                    screenX = (windowWidth - screenWidth) / 2,
                    screenY = (windowHeight - screenHeight) / 2,
                    screenWidth = screenWidth,
                    screenHeight = screenHeight
                )
            }
        }
        layoutVersion++
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
