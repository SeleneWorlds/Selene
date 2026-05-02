package com.seleneworlds.client.ui

import com.badlogic.gdx.scenes.scene2d.Actor

class HudApi(val delegate: Hud) {
    fun getActors(): List<Actor> = delegate.actors

    fun getActorsByName(): Map<String, Actor> = delegate.actorsByName

    fun getActor(name: String): Actor? = delegate.actorsByName[name]
}
