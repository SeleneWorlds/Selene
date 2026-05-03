package com.seleneworlds.client.ui

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.scenes.scene2d.ui.Stack
import com.badlogic.gdx.utils.I18NBundle
import com.badlogic.gdx.utils.viewport.ScreenViewport
import com.kotcrab.vis.ui.VisUI

class UI {

    val stage = Stage(ScreenViewport())

    val systemSkin: Skin = VisUI.load(VisUI.SkinScale.X2).let {
        VisUI.getSkin()
    }

    val bundlesRoot: Stack = Stack().apply {
        name = "Bundles"
        setFillParent(true)
    }

    val systemRoot = Stack().apply {
        name = "System"
        setFillParent(true)
    }

    val root = Stack().apply {
        name = "Root"
        add(bundlesRoot)
        add(systemRoot)
        setFillParent(true)
    }

    init {
        stage.addActor(root)

        I18NBundle.setExceptionOnMissingKey(false)
    }

    fun render() {
        stage.act(Gdx.graphics.deltaTime)
        stage.viewport.apply()
        stage.draw()
    }

    fun resize(logicalWidth: Int, logicalHeight: Int, viewportX: Int, viewportY: Int, viewportWidth: Int, viewportHeight: Int) {
        stage.viewport.setScreenBounds(viewportX, Gdx.graphics.height - viewportY - viewportHeight, viewportWidth, viewportHeight)
        stage.viewport.setWorldSize(logicalWidth.toFloat(), logicalHeight.toFloat())
        stage.viewport.apply(true)
    }

    fun dispose() {
        stage.dispose()
        VisUI.dispose()
    }

}
