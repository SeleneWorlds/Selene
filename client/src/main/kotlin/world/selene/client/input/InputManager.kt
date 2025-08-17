package world.selene.client.input

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.InputProcessor

class InputManager : InputProcessor {
    enum class InputType {
        KEYBOARD, MOUSE
    }

    fun lookupKeyboardKey(input: String): Int {
        return Input.Keys.valueOf(input)
    }

    fun lookupMouseButton(input: String): Int {
        return when (input) {
            "left" -> Input.Buttons.LEFT
            "right" -> Input.Buttons.RIGHT
            "middle" -> Input.Buttons.MIDDLE
            "back" -> Input.Buttons.BACK
            "forward" -> Input.Buttons.FORWARD
            else -> throw IllegalArgumentException("Unknown mouse button: $input")
        }
    }

    // Track pressed keys and mouse buttons
    private val pressedKeys = mutableSetOf<Int>()
    private val pressedMouseButtons = mutableSetOf<Int>()

    private val keyboardActions = mutableMapOf<Int, () -> Unit>()
    private val keyboardPressActions = mutableMapOf<Int, () -> Unit>()
    private val keyboardReleaseActions = mutableMapOf<Int, () -> Unit>()
    private val mouseActions = mutableMapOf<Int, (Int, Int) -> Unit>()
    private val mousePressActions = mutableMapOf<Int, (Int, Int) -> Unit>()
    private val mouseReleaseActions = mutableMapOf<Int, (Int, Int) -> Unit>()
    private val continuousActions = mutableMapOf<Pair<InputType, Int>, () -> Unit>()

    fun bindKeyboardAction(keyCode: Int, function: () -> Unit) {
        keyboardActions[keyCode] = function
    }

    fun bindKeyboardPressAction(keyCode: Int, function: () -> Unit) {
        keyboardPressActions[keyCode] = function
    }

    fun bindKeyboardReleaseAction(keyCode: Int, function: () -> Unit) {
        keyboardReleaseActions[keyCode] = function
    }

    fun bindMouseAction(button: String, function: (Int, Int) -> Unit) {
        val buttonCode = lookupMouseButton(button)
        mouseActions[buttonCode] = function
    }

    fun bindMousePressAction(button: String, function: (Int, Int) -> Unit) {
        val buttonCode = lookupMouseButton(button)
        mousePressActions[buttonCode] = function
    }

    fun bindMouseReleaseAction(button: String, function: (Int, Int) -> Unit) {
        val buttonCode = lookupMouseButton(button)
        mouseReleaseActions[buttonCode] = function
    }

    fun isKeyPressed(key: String): Boolean {
        val keyCode = lookupKeyboardKey(key)
        return pressedKeys.contains(keyCode)
    }

    fun isMousePressed(button: String): Boolean {
        val buttonCode = lookupMouseButton(button)
        return pressedMouseButtons.contains(buttonCode)
    }

    fun bindContinuousAction(type: InputType, input: String, function: () -> Unit) {
        when (type) {
            InputType.KEYBOARD -> {
                val keyCode = lookupKeyboardKey(input)
                if (keyCode == -1) {
                    throw IllegalArgumentException("Unknown keyboard key: $input")
                }
                continuousActions[type to keyCode] = function
            }

            InputType.MOUSE -> {
                val button = lookupMouseButton(input)
                if (button == -1) {
                    throw IllegalArgumentException("Unknown mouse button: $input")
                }
                continuousActions[type to button] = function
            }
        }
    }

    fun update(delta: Float) {
        for ((keyCode, action) in keyboardActions) {
            if (Gdx.input.isKeyJustPressed(keyCode)) {
                action()
            }
        }

        for ((key, action) in continuousActions) {
            val (type, input) = key
            when (type) {
                InputType.KEYBOARD -> {
                    if (pressedKeys.contains(input)) {
                        action()
                    }
                }

                InputType.MOUSE -> {
                    if (pressedMouseButtons.contains(input)) {
                        action()
                    }
                }
            }
        }
    }

    override fun keyDown(keycode: Int): Boolean {
        pressedKeys.add(keycode)
        keyboardPressActions[keycode]?.invoke()
        return false
    }

    override fun keyUp(keycode: Int): Boolean {
        pressedKeys.remove(keycode)
        keyboardReleaseActions[keycode]?.invoke()
        return false
    }

    override fun keyTyped(character: Char): Boolean {
        return false
    }

    override fun touchDown(
        screenX: Int,
        screenY: Int,
        pointer: Int,
        button: Int
    ): Boolean {
        pressedMouseButtons.add(button)
        mousePressActions[button]?.invoke(screenX, screenY)
        return false
    }

    override fun touchUp(
        screenX: Int,
        screenY: Int,
        pointer: Int,
        button: Int
    ): Boolean {
        pressedMouseButtons.remove(button)
        mouseActions[button]?.invoke(screenX, screenY)
        mouseReleaseActions[button]?.invoke(screenX, screenY)
        return false
    }

    override fun touchCancelled(
        screenX: Int,
        screenY: Int,
        pointer: Int,
        button: Int
    ): Boolean {
        pressedMouseButtons.remove(button)
        return false
    }

    override fun touchDragged(screenX: Int, screenY: Int, pointer: Int): Boolean {
        return false
    }

    override fun mouseMoved(screenX: Int, screenY: Int): Boolean {
        return false
    }

    override fun scrolled(amountX: Float, amountY: Float): Boolean {
        return false
    }
}