package com.seleneworlds.client.ui.cef

import com.badlogic.gdx.Input
import com.badlogic.gdx.InputAdapter
import com.seleneworlds.client.window.WindowManager

class CefInputProcessor(
    private val cef: CefBrowserUi,
    private val interactionState: CefInteractionState,
    private val windowManager: WindowManager
) : InputAdapter() {
    private val capturedButtons = mutableSetOf<Int>()
    private val pressedKeys = mutableSetOf<Int>()
    private var mouseX = 0
    private var mouseY = 0

    override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        if (!cef.enabled || pointer != 0) return false
        val (x, y) = browserCoordinates(screenX, screenY) ?: return false
        if (!interactionState.isInteractive(x, y)) return false
        capturedButtons += button
        dispatchMouse("mousedown", x, y, button)
        return true
    }

    override fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        if (!cef.enabled || pointer != 0 || button !in capturedButtons) return false
        val (x, y) = browserCoordinatesClamped(screenX, screenY)
        dispatchMouse("mouseup", x, y, button)
        dispatchMouse("click", x, y, button)
        capturedButtons -= button
        return true
    }

    override fun touchCancelled(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean =
        touchUp(screenX, screenY, pointer, button)

    override fun touchDragged(screenX: Int, screenY: Int, pointer: Int): Boolean {
        if (!cef.enabled || pointer != 0 || capturedButtons.isEmpty()) return false
        val (x, y) = browserCoordinatesClamped(screenX, screenY)
        dispatchMouse("mousemove", x, y, capturedButtons.first())
        return true
    }

    override fun mouseMoved(screenX: Int, screenY: Int): Boolean {
        if (!cef.enabled) return false
        val coordinates = browserCoordinates(screenX, screenY) ?: return false
        mouseX = coordinates.first
        mouseY = coordinates.second
        dispatchMouse("mousemove", mouseX, mouseY, -1)
        return interactionState.isInteractive(mouseX, mouseY)
    }

    override fun scrolled(amountX: Float, amountY: Float): Boolean {
        if (!cef.enabled || !interactionState.isInteractive(mouseX, mouseY)) return false
        cef.dispatchPointer("wheel", mouseX, mouseY, 0, buttonMask(), amountX * 100f, amountY * 100f)
        return true
    }

    override fun keyDown(keycode: Int): Boolean {
        if (!cef.enabled) return false
        pressedKeys += keycode
        val key = browserKey(keycode) ?: return false
        if (interactionState.hasUi()) dispatchKeyboard("keydown", key)
        return interactionState.consumesKey(key)
    }

    override fun keyUp(keycode: Int): Boolean {
        if (!cef.enabled) return false
        val key = browserKey(keycode)
        if (key != null && interactionState.hasUi()) dispatchKeyboard("keyup", key)
        val consumed = key != null && interactionState.consumesKey(key)
        pressedKeys -= keycode
        return consumed
    }

    override fun keyTyped(character: Char): Boolean {
        if (!cef.enabled || !interactionState.hasUi() || !interactionState.capturesText() ||
            character.isISOControl()
        ) return false
        if (interactionState.hasEditableFocus()) {
            cef.insertText(character)
        } else {
            cef.dispatchCharacter(character, hasShift(), hasControl(), hasAlt(), hasMeta())
        }
        return true
    }

    private fun dispatchMouse(type: String, x: Int, y: Int, gdxButton: Int) {
        cef.dispatchPointer(type, x, y, browserButton(gdxButton), buttonMask())
    }

    private fun browserCoordinates(screenX: Int, screenY: Int): Pair<Int, Int>? {
        val viewport = windowManager.uiViewport
        if (screenX < viewport.screenX || screenY < viewport.screenY ||
            screenX >= viewport.screenX + viewport.screenWidth || screenY >= viewport.screenY + viewport.screenHeight
        ) return null
        return ((screenX - viewport.screenX) * viewport.logicalWidth / viewport.screenWidth) to
                ((screenY - viewport.screenY) * viewport.logicalHeight / viewport.screenHeight)
    }

    private fun browserCoordinatesClamped(screenX: Int, screenY: Int): Pair<Int, Int> {
        val viewport = windowManager.uiViewport
        if (viewport.screenWidth <= 0 || viewport.screenHeight <= 0) {
            return 0 to 0
        }
        val x = ((screenX - viewport.screenX) * viewport.logicalWidth / viewport.screenWidth)
            .coerceIn(0, viewport.logicalWidth - 1)
        val y = ((screenY - viewport.screenY) * viewport.logicalHeight / viewport.screenHeight)
            .coerceIn(0, viewport.logicalHeight - 1)
        return x to y
    }

    private fun browserButton(button: Int): Int = when (button) {
        Input.Buttons.LEFT -> 0
        Input.Buttons.MIDDLE -> 1
        Input.Buttons.RIGHT -> 2
        else -> -1
    }

    private fun buttonMask(): Int = capturedButtons.fold(0) { mask, button ->
        mask or when (button) {
            Input.Buttons.LEFT -> 1
            Input.Buttons.RIGHT -> 2
            Input.Buttons.MIDDLE -> 4
            else -> 0
        }
    }

    private fun dispatchKeyboard(type: String, key: String) {
        cef.dispatchKeyboard(type, key, hasShift(), hasControl(), hasAlt(), hasMeta())
    }

    private fun hasShift() = Input.Keys.SHIFT_LEFT in pressedKeys || Input.Keys.SHIFT_RIGHT in pressedKeys
    private fun hasControl() = Input.Keys.CONTROL_LEFT in pressedKeys || Input.Keys.CONTROL_RIGHT in pressedKeys
    private fun hasAlt() = Input.Keys.ALT_LEFT in pressedKeys || Input.Keys.ALT_RIGHT in pressedKeys
    private fun hasMeta() = Input.Keys.SYM in pressedKeys

    private fun browserKey(keycode: Int): String? = when (keycode) {
        Input.Keys.ENTER -> "Enter"
        Input.Keys.ESCAPE -> "Escape"
        Input.Keys.BACKSPACE -> "Backspace"
        Input.Keys.TAB -> "Tab"
        Input.Keys.FORWARD_DEL -> "Delete"
        Input.Keys.HOME -> "Home"
        Input.Keys.END -> "End"
        Input.Keys.PAGE_UP -> "PageUp"
        Input.Keys.PAGE_DOWN -> "PageDown"
        Input.Keys.LEFT -> "ArrowLeft"
        Input.Keys.RIGHT -> "ArrowRight"
        Input.Keys.UP -> "ArrowUp"
        Input.Keys.DOWN -> "ArrowDown"
        Input.Keys.SHIFT_LEFT, Input.Keys.SHIFT_RIGHT -> "Shift"
        Input.Keys.CONTROL_LEFT, Input.Keys.CONTROL_RIGHT -> "Control"
        Input.Keys.ALT_LEFT, Input.Keys.ALT_RIGHT -> "Alt"
        Input.Keys.SYM -> "Meta"
        else -> null
    }
}
