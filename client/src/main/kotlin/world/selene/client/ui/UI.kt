package world.selene.client.ui

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.*
import com.badlogic.gdx.utils.I18NBundle
import com.badlogic.gdx.utils.viewport.ScreenViewport
import com.kotcrab.vis.ui.VisUI
import org.slf4j.Logger
import world.selene.client.assets.AssetProvider

class UI(private val assetProvider: AssetProvider, private val logger: Logger) {

    val stage = Stage(ScreenViewport())

    val systemSkin: Skin = VisUI.load(VisUI.SkinScale.X2).let {
        VisUI.getSkin()
    }

    val bundlesRoot = Stack().apply {
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
        // TODO stage is an InputProcessor - need to use a multiplexer
    }

    fun render() {
        stage.act(Gdx.graphics.deltaTime)
        stage.draw()
    }

    fun resize(width: Int, height: Int) {
        //stage.viewport.update(width, height, true)
    }

    fun dispose() {
        stage.dispose()
        VisUI.dispose()
    }

}
