package com.seleneworlds.client.ui

import com.badlogic.gdx.scenes.scene2d.Actor
import com.seleneworlds.common.script.ExposedApi

class Hud(
    val actors: List<Actor>,
    val actorsByName: Map<String, Actor>
) : ExposedApi<HudApi> {
    override val api = HudApi(this)
}
