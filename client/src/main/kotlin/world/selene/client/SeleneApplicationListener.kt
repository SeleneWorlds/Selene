package world.selene.client

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
import world.selene.client.camera.CameraManager
import world.selene.client.controls.GridMovement
import world.selene.client.controls.PlayerController
import world.selene.client.input.InputManager
import world.selene.client.lua.ClientLuaSignals
import world.selene.client.maps.ClientMap
import world.selene.client.network.NetworkClient
import world.selene.client.rendering.DebugRenderer
import world.selene.client.rendering.drawable.DrawableManager
import world.selene.client.rendering.SceneRenderer
import world.selene.client.ui.UI
import world.selene.client.rendering.visual.VisualManager
import world.selene.common.threading.MainThreadDispatcher
import world.selene.common.util.Disposable

class SeleneApplicationListener(
    private val client: SeleneClient,
    private val networkClient: NetworkClient,
    private val visualManager: VisualManager,
    private val cameraManager: CameraManager,
    private val inputMultiplexer: InputMultiplexer,
    private val inputManager: InputManager,
    private val ui: UI,
    private val clientMap: ClientMap,
    private val playerController: PlayerController,
    private val drawableManager: DrawableManager,
    private val gridMovement: GridMovement,
    private val sceneRenderer: SceneRenderer,
    private val debugRenderer: DebugRenderer,
    private val signals: ClientLuaSignals,
    private val mainThreadDispatcher: MainThreadDispatcher
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

        inputMultiplexer.addProcessor(ui.stage)
        inputMultiplexer.addProcessor(inputManager)
        Gdx.input.inputProcessor = inputMultiplexer
    }

    override fun resize(width: Int, height: Int) {
        ui.resize(width, height)
    }

    override fun render() {
        mainThreadDispatcher.process()

        signals.gamePreTick.emit()

        networkClient.processWork()

        val delta = Gdx.graphics.deltaTime
        inputManager.update(delta)
        gridMovement.update(delta)
        cameraManager.update(delta)
        drawableManager.update(delta)

        ScreenUtils.clear(0f, 0f, 0f, 0f)

        spriteBatch.projectionMatrix = cameraManager.camera.combined
        spriteBatch.begin()
        sceneRenderer.render(spriteBatch)
        spriteBatch.end()

        ui.render()

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
}