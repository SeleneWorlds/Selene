package com.seleneworlds.client

import com.badlogic.gdx.ApplicationListener
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.InputMultiplexer
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.utils.ScreenUtils
import org.koin.mp.KoinPlatform.getKoin
import com.seleneworlds.client.bundle.ClientBundleWatcher
import com.seleneworlds.client.camera.CameraManager
import com.seleneworlds.client.controls.GridMovement
import com.seleneworlds.client.game.ClientEvents
import com.seleneworlds.client.input.InputManager
import com.seleneworlds.client.input.SystemInputProcessor
import com.seleneworlds.client.network.NetworkClient
import com.seleneworlds.client.rendering.DebugRenderer
import com.seleneworlds.client.rendering.SceneRenderer
import com.seleneworlds.client.rendering.drawable.DrawableManager
import com.seleneworlds.client.ui.UI
import com.seleneworlds.client.window.WindowManager
import com.seleneworlds.common.threading.MainThreadDispatcher
import com.seleneworlds.common.util.Disposable

class SeleneApplicationListener(
    private val client: SeleneClient,
    private val networkClient: NetworkClient,
    private val cameraManager: CameraManager,
    private val inputMultiplexer: InputMultiplexer,
    private val inputManager: InputManager,
    private val ui: UI,
    private val drawableManager: DrawableManager,
    private val gridMovement: GridMovement,
    private val sceneRenderer: SceneRenderer,
    private val debugRenderer: DebugRenderer,
    private val bundleWatcher: ClientBundleWatcher,
    private val mainThreadDispatcher: MainThreadDispatcher,
    private val windowManager: WindowManager
) : ApplicationListener {

    lateinit var systemFont: BitmapFont
    lateinit var spriteBatch: SpriteBatch
    lateinit var markerTexture: Texture

    override fun create() {
        client.start()

        debugRenderer.initialize()
        spriteBatch = SpriteBatch()
        markerTexture = Texture("icon_16.png")
        systemFont = BitmapFont()

        inputMultiplexer.addProcessor(SystemInputProcessor(windowManager))
        inputMultiplexer.addProcessor(ui.stage)
        inputMultiplexer.addProcessor(inputManager)
        Gdx.input.inputProcessor = inputMultiplexer

        windowManager.updateWindowSize(Gdx.graphics.width, Gdx.graphics.height)
        applyWindowLayout()
    }

    override fun resize(width: Int, height: Int) {
        windowManager.updateWindowSize(width, height)
        applyWindowLayout()
    }

    override fun render() {
        applyWindowLayout()
        mainThreadDispatcher.process()

        ClientEvents.GamePreTick.EVENT.invoker().gamePreTick()

        networkClient.processWork()
        bundleWatcher.processPendingUpdates()

        val delta = Gdx.graphics.deltaTime
        inputManager.update()
        gridMovement.update()
        cameraManager.update()
        drawableManager.update(delta)

        ScreenUtils.clear(0f, 0f, 0f, 0f)

        cameraManager.applyRenderViewport()
        spriteBatch.projectionMatrix = cameraManager.camera.combined
        spriteBatch.begin()
        sceneRenderer.render(spriteBatch)
        spriteBatch.end()

        ui.render()

        cameraManager.applyRenderViewport()
        debugRenderer.render(cameraManager.camera.combined)

        if (Vector2.Zero.x != 0f || Vector2.Zero.y != 0f) {
            throw IllegalStateException("Vector2.Zero is not zero")
        } else if (Vector3.Zero.x != 0f || Vector3.Zero.y != 0f || Vector3.Zero.z != 0f) {
            throw IllegalStateException("Vector3.Zero is not zero")
        }
    }

    override fun pause() {
    }

    override fun resume() {
    }

    override fun dispose() {
        spriteBatch.dispose()
        ui.dispose()
        networkClient.disconnect()

        getKoin().getAll<Disposable>().forEach { it.dispose() }
    }

    private fun applyWindowLayout() {
        val viewport = windowManager.viewport
        cameraManager.resize(viewport)
        ui.resize(
            logicalWidth = viewport.logicalWidth,
            logicalHeight = viewport.logicalHeight,
            viewportX = viewport.screenX,
            viewportY = viewport.screenY,
            viewportWidth = viewport.screenWidth,
            viewportHeight = viewport.screenHeight
        )
    }
}
