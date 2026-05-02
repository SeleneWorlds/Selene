package com.seleneworlds.client.input

import com.badlogic.gdx.Gdx

class InputApi(private val inputManager: InputManager) {

    val inputTypeKeyboard: InputType = InputType.KEYBOARD
    val inputTypeMouse: InputType = InputType.MOUSE

    fun bindContinuousAction(type: InputType, input: String, callback: () -> Unit) {
        inputManager.bindContinuousAction(type, input, callback)
    }

    fun bindAction(type: InputType, input: String, keyboardCallback: () -> Unit, mouseCallback: (Int, Int) -> Unit) {
        when (type) {
            InputType.KEYBOARD -> {
                val keyCode = inputManager.lookupKeyboardKey(input)
                if (keyCode == -1) {
                    throw IllegalArgumentException("Unknown keyboard key: $input")
                }
                inputManager.bindKeyboardAction(keyCode, keyboardCallback)
            }

            InputType.MOUSE -> inputManager.bindMouseAction(input, mouseCallback)
        }
    }

    fun bindPressAction(type: InputType, input: String, keyboardCallback: () -> Unit, mouseCallback: (Int, Int) -> Unit) {
        when (type) {
            InputType.KEYBOARD -> {
                val keyCode = inputManager.lookupKeyboardKey(input)
                if (keyCode == -1) {
                    throw IllegalArgumentException("Unknown keyboard key: $input")
                }
                inputManager.bindKeyboardPressAction(keyCode, keyboardCallback)
            }

            InputType.MOUSE -> inputManager.bindMousePressAction(input, mouseCallback)
        }
    }

    fun bindReleaseAction(type: InputType, input: String, keyboardCallback: () -> Unit, mouseCallback: (Int, Int) -> Unit) {
        when (type) {
            InputType.KEYBOARD -> {
                val keyCode = inputManager.lookupKeyboardKey(input)
                if (keyCode == -1) {
                    throw IllegalArgumentException("Unknown keyboard key: $input")
                }
                inputManager.bindKeyboardReleaseAction(keyCode, keyboardCallback)
            }

            InputType.MOUSE -> inputManager.bindMouseReleaseAction(input, mouseCallback)
        }
    }

    fun isKeyPressed(key: String): Boolean = inputManager.isKeyPressed(key)

    fun isMousePressed(button: String): Boolean = inputManager.isMousePressed(button)

    fun getMousePosition(): Pair<Int, Int> = Gdx.input.x to Gdx.input.y
}
