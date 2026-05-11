package com.seleneworlds.client

import com.badlogic.gdx.ApplicationListener
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.InputMultiplexer
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.graphics.glutils.FrameBuffer
import com.badlogic.gdx.graphics.Pixmap
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
import com.seleneworlds.client.window.WindowViewport
import com.seleneworlds.client.window.WindowManager
import com.seleneworlds.common.threading.MainThreadDispatcher
import com.seleneworlds.common.util.Disposable

class SeleneApplicationListener(
    private val client: SeleneClient,
    private val clientReloadManager: ClientReloadManager,
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
    private val screenProjection = Matrix4()
    private var worldFrameBuffer: FrameBuffer? = null
    private var worldFrameRegion: TextureRegion? = null

    override fun create() {
        client.start()

        debugRenderer.initialize()
        spriteBatch = SpriteBatch()
        systemFont = BitmapFont()

        inputMultiplexer.addProcessor(SystemInputProcessor(windowManager, clientReloadManager))
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

        if (windowManager.isOffscreenRendering) {
            renderWorldToFramebuffer(windowManager.viewport)
        } else {
            renderWorldDirect()
        }

        ui.render()

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
        worldFrameBuffer?.dispose()
        ui.dispose()
        networkClient.disconnect()

        getKoin().getAll<Disposable>().forEach { it.dispose() }
    }

    private fun applyWindowLayout() {
        val viewport = windowManager.viewport
        ensureWorldFramebuffer(viewport)
        cameraManager.resize(viewport)
        val uiViewport = windowManager.uiViewport
        ui.resize(
            logicalWidth = uiViewport.logicalWidth,
            logicalHeight = uiViewport.logicalHeight,
            viewportX = uiViewport.screenX,
            viewportY = uiViewport.screenY,
            viewportWidth = uiViewport.screenWidth,
            viewportHeight = uiViewport.screenHeight
        )
    }

    private fun renderWorldDirect() {
        cameraManager.applyRenderViewport()
        spriteBatch.projectionMatrix = cameraManager.camera.combined
        spriteBatch.begin()
        sceneRenderer.render(spriteBatch)
        spriteBatch.end()

        cameraManager.applyRenderViewport()
        debugRenderer.render(cameraManager.camera.combined)
    }

    private fun renderWorldToFramebuffer(viewport: WindowViewport) {
        val frameBuffer = worldFrameBuffer ?: return renderWorldDirect()
        frameBuffer.begin()
        Gdx.gl.glClearColor(0f, 0f, 0f, 0f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

        cameraManager.applyLogicalRenderViewport()
        spriteBatch.projectionMatrix = cameraManager.camera.combined
        spriteBatch.begin()
        sceneRenderer.render(spriteBatch)
        spriteBatch.end()

        cameraManager.applyLogicalRenderViewport()
        debugRenderer.render(cameraManager.camera.combined)
        frameBuffer.end()

        Gdx.gl.glViewport(0, 0, Gdx.graphics.width, Gdx.graphics.height)
        spriteBatch.projectionMatrix = screenProjection.setToOrtho2D(
            0f,
            0f,
            Gdx.graphics.width.toFloat(),
            Gdx.graphics.height.toFloat()
        )
        spriteBatch.begin()
        worldFrameRegion?.let { region ->
            spriteBatch.draw(
                region,
                viewport.screenX.toFloat(),
                (Gdx.graphics.height - viewport.screenY - viewport.screenHeight).toFloat(),
                viewport.screenWidth.toFloat(),
                viewport.screenHeight.toFloat()
            )
        }
        spriteBatch.end()
    }

    private fun ensureWorldFramebuffer(viewport: WindowViewport) {
        if (!windowManager.isOffscreenRendering) {
            worldFrameBuffer?.dispose()
            worldFrameBuffer = null
            worldFrameRegion = null
            return
        }

        val current = worldFrameBuffer
        if (current != null && current.width == viewport.logicalWidth && current.height == viewport.logicalHeight) {
            return
        }

        current?.dispose()
        worldFrameBuffer = FrameBuffer(Pixmap.Format.RGBA8888, viewport.logicalWidth, viewport.logicalHeight, false).also { frameBuffer ->
            frameBuffer.colorBufferTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)
            worldFrameRegion = TextureRegion(frameBuffer.colorBufferTexture).apply {
                flip(false, true)
            }
        }
    }
}
