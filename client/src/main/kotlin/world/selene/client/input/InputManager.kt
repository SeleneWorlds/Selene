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
    private val continuousActions = mutableMapOf<Pair<InputType, Int>, () -> Unit>()

    fun bindKeyboardAction(keyCode: Int, function: () -> Unit) {
        keyboardActions[keyCode] = function
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
        return false
    }

    override fun keyUp(keycode: Int): Boolean {
        pressedKeys.remove(keycode)
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
        return false
    }

    override fun touchUp(
        screenX: Int,
        screenY: Int,
        pointer: Int,
        button: Int
    ): Boolean {
        pressedMouseButtons.remove(button)
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