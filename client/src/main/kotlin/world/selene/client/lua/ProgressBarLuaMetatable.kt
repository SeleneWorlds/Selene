package world.selene.client.lua

import com.badlogic.gdx.scenes.scene2d.ui.ProgressBar

object ProgressBarLuaMetatable {
    val luaMeta = ActorLuaMetatable.luaMeta.extend(ProgressBar::class) {
        getter(ProgressBar::getValue)
        setter(ProgressBar::setValue)
    }
}