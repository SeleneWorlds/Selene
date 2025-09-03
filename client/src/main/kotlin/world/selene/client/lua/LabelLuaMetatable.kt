package world.selene.client.lua

import com.badlogic.gdx.scenes.scene2d.ui.Label
import world.selene.common.lua.checkString

object LabelLuaMetatable {
    val luaMeta = ActorLuaMetatable.luaMeta.extend(Label::class) {
        getter("Text") {
            val label = it.checkSelf()
            it.push(label.text.toString())
            1
        }
        setter("Text") {
            val label = it.checkSelf()
            val text = it.checkString(3)
            label.setText(text)
            0
        }
    }
}