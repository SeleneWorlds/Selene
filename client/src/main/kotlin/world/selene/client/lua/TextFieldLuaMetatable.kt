package world.selene.client.lua

import com.badlogic.gdx.scenes.scene2d.ui.TextField
import world.selene.common.lua.checkString

object TextFieldLuaMetatable {
    val luaMeta = ActorLuaMetatable.luaMeta.extend(TextField::class) {
        getter(TextField::getDefaultInputListener, "InputListener")
        getter("Text") {
            val textField = it.checkSelf()
            it.push(textField.text.toString())
            1
        }
        setter("Text") {
            val textField = it.checkSelf()
            val text = it.checkString(3)
            textField.setText(text)
            0
        }
    }
}