package world.selene.client

import com.badlogic.gdx.ApplicationListener
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.utils.ScreenUtils
import ktx.assets.async.AssetStorage
import world.selene.client.camera.CameraManager
import world.selene.client.config.ClientRuntimeConfig
import world.selene.client.controls.GridMovement
import world.selene.client.controls.PlayerController
import world.selene.client.grid.Grid
import world.selene.client.input.InputManager
import world.selene.client.lua.ClientLuaSignals
import world.selene.client.maps.ClientMap
import world.selene.client.network.NetworkClient
import world.selene.client.rendering.DebugRenderer
import world.selene.client.rendering.SceneRenderer
import world.selene.client.ui.UI
import world.selene.client.visual.VisualManager
import world.selene.common.bundles.BundleLoader

class SeleneApplicationListener(
    private val client: SeleneClient,
    private val assetStorage: AssetStorage,
    private val bundleLoader: BundleLoader,
    private val networkClient: NetworkClient,
    private val runtimeConfig: ClientRuntimeConfig,
    private val visualManager: VisualManager,
    private val cameraManager: CameraManager,
    private val inputManager: InputManager,
    private val grid: Grid,
    private val ui: UI,
    private val clientMap: ClientMap,
    private val playerController: PlayerController,
    private val gridMovement: GridMovement,
    private val sceneRenderer: SceneRenderer,
    private val debugRenderer: DebugRenderer,
    private val signals: ClientLuaSignals
) : ApplicationListener {

    lateinit var systemFont: BitmapFont
    lateinit var spriteBatch: SpriteBatch
    lateinit var markerTexture: Texture

    override fun create() {
        debugRenderer.initialize()
        spriteBatch = SpriteBatch()
        markerTexture = Texture("icon_16.png")
        systemFont = BitmapFont(true)
        Gdx.input.inputProcessor = inputManager

        inputManager.bindKeyboardAction(Input.Keys.I) {
            playerController.controlledEntity?.let { entity ->
                clientMap.getTilesAt(entity.coordinate).forEach {
                    println("${it.tileName} ${it.visualInstance?.sortLayerOffset} at ${it.sortLayer}")
                }
            }
        }
    }

    override fun resize(width: Int, height: Int) {
        ui.resize(width, height)
    }

    override fun render() {
        signals.gamePreTick.emit { lua ->
            0
        }

        networkClient.processWork()

        val delta = Gdx.graphics.deltaTime
        inputManager.update(delta)
        gridMovement.update(delta)
        cameraManager.update(delta)
        visualManager.updateShared(delta)

        ScreenUtils.clear(0f, 0f, 0f, 0f)

        spriteBatch.projectionMatrix = cameraManager.camera.combined
        spriteBatch.begin()
        sceneRenderer.render(spriteBatch)
        spriteBatch.end()

        ui.render()

        debugRenderer.render(cameraManager.camera.combined)
    }

    override fun pause() {
    }

    override fun resume() {
    }

    override fun dispose() {
        spriteBatch.dispose()
        ui.dispose()
        networkClient.disconnect()
    }
}