package com.seleneworlds.client.ui.cef

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Matrix4
import com.seleneworlds.client.config.ClientConfig
import com.seleneworlds.client.window.WindowViewport
import com.seleneworlds.common.util.Disposable
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import me.friwi.jcefmaven.CefAppBuilder
import me.friwi.jcefmaven.MavenCefAppHandlerAdapter
import org.cef.CefApp
import org.cef.CefClient
import org.cef.CefSettings
import org.cef.browser.CefBrowser
import org.cef.browser.CefFrame
import org.cef.handler.CefDisplayHandlerAdapter
import org.cef.handler.CefLoadHandler
import org.cef.handler.CefLoadHandlerAdapter
import org.cef.handler.CefLifeSpanHandlerAdapter
import org.cef.handler.CefRequestHandler
import org.cef.handler.CefRequestHandlerAdapter
import org.cef.network.CefRequest
import org.slf4j.Logger
import java.awt.BorderLayout
import java.awt.EventQueue
import java.awt.Dimension
import java.awt.Window
import java.io.File
import java.nio.ByteBuffer
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.TimeUnit
import java.util.concurrent.CountDownLatch
import javax.swing.JFrame

class CefBrowserUi(
    private val config: ClientConfig,
    private val logger: Logger,
    private val bundleUiSource: BundleUiSource,
    private val bridge: CefUiBridge,
    private val bundleScheme: CefBundleScheme
) : Disposable {
    private val mailbox = CefOverlayFrameMailbox()
    private val projection = Matrix4()
    private var app: CefApp? = null
    private var client: CefClient? = null
    private var browser: CefBrowser? = null
    private var hostFrame: JFrame? = null
    private var texture: Texture? = null
    private var region: TextureRegion? = null
    private var uploadBuffer: ByteBuffer? = null
    private var pageDirectory: Path? = null
    private var initialized = false
    private val receivedFirstFrame = AtomicBoolean()
    private val reloadRequested = AtomicBoolean()
    private var browserClosed = CountDownLatch(1)
    private var appTerminated = CountDownLatch(1)
    private var browserWidth = 0
    private var browserHeight = 0

    val enabled: Boolean get() = config.browserUiEnabled

    fun initialize(viewport: WindowViewport) {
        if (!enabled || initialized) return
        try {
            initializeBrowser(viewport)
        } catch (error: Throwable) {
            logger.error("Browser UI initialization failed; continuing without browser UI", error)
            dispose()
        }
    }

    private fun initializeBrowser(viewport: WindowViewport) {
        val entries = loadEntrypoints()
        if (entries.isEmpty()) {
            logger.info("No enabled bundle UI entrypoints were found; continuing without browser UI")
            return
        }
        val builder = CefAppBuilder().apply {
            setInstallDir(File(config.cefInstallDir))
            cefSettings.windowless_rendering_enabled = true
            cefSettings.background_color = cefSettings.ColorType(0, 0, 0, 0)
            cefSettings.root_cache_path = File(config.cefInstallDir, "cache").absolutePath
            setAppHandler(object : MavenCefAppHandlerAdapter() {
                override fun stateHasChanged(state: CefApp.CefAppState) {
                    if (state == CefApp.CefAppState.TERMINATED) appTerminated.countDown()
                }
            })
            addJcefArgs("--autoplay-policy=no-user-gesture-required")
        }
        val cefClient: CefClient
        app = builder.build().also {
            cefClient = it.createClient()
            check(it.registerSchemeHandlerFactory(
                CefBundleScheme.SCHEME,
                CefBundleScheme.HOST,
                bundleScheme
            )) { "Failed to install the browser UI resource handler" }
        }

        client = cefClient.apply {
            addMessageRouter(bridge.initialize())
            addLifeSpanHandler(object : CefLifeSpanHandlerAdapter() {
                override fun onBeforeClose(closingBrowser: CefBrowser) {
                    if (closingBrowser === browser) browserClosed.countDown()
                }
            })
            addDisplayHandler(object : CefDisplayHandlerAdapter() {
                override fun onConsoleMessage(browser: CefBrowser, level: CefSettings.LogSeverity,
                                              message: String, source: String, line: Int): Boolean {
                    logger.info("CEF console [{}] {}:{} {}", level, source, line, message)
                    return true
                }
            })
            addLoadHandler(object : CefLoadHandlerAdapter() {
                override fun onLoadError(browser: CefBrowser, frame: CefFrame, errorCode: CefLoadHandler.ErrorCode,
                                         errorText: String, failedUrl: String) {
                    logger.error("CEF failed to load {}: {} ({})", failedUrl, errorText, errorCode)
                }
            })
            addRequestHandler(object : CefRequestHandlerAdapter() {
                override fun onBeforeBrowse(browser: CefBrowser, frame: CefFrame, request: CefRequest,
                    userGesture: Boolean, isRedirect: Boolean): Boolean {
                    if (!frame.isMain || CefTrustedDocuments.contains(request.url)) return false
                    logger.warn("Blocked untrusted CEF top-level {} to {}",
                        if (isRedirect) "redirect" else "navigation", request.url)
                    return true
                }

                override fun onOpenURLFromTab(browser: CefBrowser, frame: CefFrame, targetUrl: String,
                    userGesture: Boolean): Boolean {
                    if (CefTrustedDocuments.contains(targetUrl)) return false
                    logger.warn("Blocked untrusted CEF top-level navigation to {}", targetUrl)
                    return true
                }

                override fun onRenderProcessTerminated(browser: CefBrowser,
                                                       status: CefRequestHandler.TerminationStatus, errorCode: Int, errorString: String) {
                    logger.error("CEF renderer terminated: {} ({}: {})", status, errorCode, errorString)
                    requestReload()
                }
            })
        }

        browser = cefClient.createBrowser(writeBrowserPage(entries), true, true).also { cefBrowser ->
            cefBrowser.renderHandler.setOnPaintListener { event ->
                if (!event.popup) {
                    mailbox.publish(event.renderedFrame, event.width, event.height)
                    if (receivedFirstFrame.compareAndSet(false, true)) {
                        logger.info("CEF produced its first {}x{} off-screen frame", event.width, event.height)
                    }
                }
            }
            cefBrowser.setWindowlessFrameRate(60)
        }

        EventQueue.invokeAndWait {
            val component = browser!!.uiComponent
            component.preferredSize = Dimension(viewport.logicalWidth, viewport.logicalHeight)
            component.setSize(viewport.logicalWidth, viewport.logicalHeight)
            hostFrame = JFrame().apply {
                isUndecorated = true
                opacity = 0f
                type = Window.Type.UTILITY
                layout = BorderLayout()
                add(component, BorderLayout.CENTER)
                pack()
                setLocation(-32000, -32000)
                isVisible = true
                validate()
            }
            browser!!.createImmediately()
        }
        browserWidth = viewport.logicalWidth
        browserHeight = viewport.logicalHeight
        initialized = true
    }

    fun resize(viewport: WindowViewport) {
        if (!initialized || (browserWidth == viewport.logicalWidth && browserHeight == viewport.logicalHeight)) return
        browserWidth = viewport.logicalWidth
        browserHeight = viewport.logicalHeight
        EventQueue.invokeLater {
            browser?.uiComponent?.setSize(viewport.logicalWidth, viewport.logicalHeight)
            hostFrame?.setSize(viewport.logicalWidth, viewport.logicalHeight)
        }
    }

    fun awaitUiReady() {
        if (!initialized) return
        if (!bridge.awaitUiReady(UI_READY_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
            logger.error("CEF bundle UI did not become ready within {} seconds; continuing without it",
                UI_READY_TIMEOUT_SECONDS)
            dispose()
            return
        }
        logger.info("CEF bundle UI is ready; continuing with server join")
    }

    fun requestReload() {
        if (enabled) reloadRequested.set(true)
    }

    fun processPendingReload() {
        if (!initialized || !reloadRequested.compareAndSet(true, false)) return
        try {
            val entries = loadEntrypoints()
            if (entries.isEmpty()) {
                logger.warn("Browser UI reload found no enabled entrypoints; keeping the current UI")
                return
            }
            val currentBrowser = browser ?: return
            bridge.prepareReload(currentBrowser)
            currentBrowser.loadURL(writeBrowserPage(entries))
            logger.info("Reloading {} browser UI entrypoint(s)", entries.size)
        } catch (error: Exception) {
            logger.error("Failed to reload browser UI; keeping the current UI", error)
        }
    }

    fun render(batch: SpriteBatch, viewport: WindowViewport) {
        if (!initialized) return
        uploadLatestFrame()
        val currentRegion = region ?: return
        Gdx.gl.glViewport(0, 0, Gdx.graphics.width, Gdx.graphics.height)
        batch.projectionMatrix = projection.setToOrtho2D(0f, 0f, Gdx.graphics.width.toFloat(), Gdx.graphics.height.toFloat())
        batch.setColor(1f, 1f, 1f, 1f)
        batch.begin()
        batch.draw(currentRegion, viewport.screenX.toFloat(),
            (Gdx.graphics.height - viewport.screenY - viewport.screenHeight).toFloat(),
            viewport.screenWidth.toFloat(), viewport.screenHeight.toFloat())
        batch.end()
    }

    fun dispatchPointer(type: String, x: Int, y: Int, button: Int, buttons: Int, deltaX: Float = 0f,
        deltaY: Float = 0f) {
        if (!initialized || type !in POINTER_EVENT_TYPES) return
        browser?.executeJavaScript(
            "window.__seleneInput?.pointer('$type',$x,$y,$button,$buttons,$deltaX,$deltaY)", "", 0)
    }

    fun insertText(character: Char) {
        if (!initialized) return
        val encoded = Json.encodeToString(String.serializer(), character.toString())
        browser?.executeJavaScript("document.execCommand('insertText',false,$encoded)", "", 0)
    }

    fun dispatchKeyboard(type: String, key: String, shift: Boolean, control: Boolean, alt: Boolean,
        meta: Boolean) {
        if (!initialized || type !in setOf("keydown", "keyup") || key !in KEY_NAMES) return
        browser?.executeJavaScript(
            "window.__seleneInput?.keyboard('$type','$key',$shift,$control,$alt,$meta)", "", 0)
    }

    fun dispatchCharacter(character: Char, shift: Boolean, control: Boolean, alt: Boolean, meta: Boolean) {
        if (!initialized || character.isISOControl()) return
        val escaped = when (character) {
            '\\' -> "\\\\"
            '\'' -> "\\'"
            else -> character.toString()
        }
        browser?.executeJavaScript(
            "window.__seleneInput?.keyboard('keydown','$escaped',$shift,$control,$alt,$meta);" +
                "window.__seleneInput?.keyboard('keyup','$escaped',$shift,$control,$alt,$meta)", "", 0)
    }

    fun focus() {
        if (!initialized) return
        EventQueue.invokeLater {
            browser?.setFocus(true)
        }
    }

    private fun uploadLatestFrame() {
        val frame = mailbox.takeLatest() ?: return
        if (texture?.width != frame.width || texture?.height != frame.height) {
            texture?.dispose()
            texture = Texture(frame.width, frame.height, Pixmap.Format.RGBA8888).apply {
                setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)
            }
            region = TextureRegion(texture)
            uploadBuffer = ByteBuffer.allocateDirect(frame.bgra.size)
        }
        val pixels = uploadBuffer ?: return
        pixels.clear()
        pixels.put(frame.bgra)
        pixels.flip()
        texture!!.bind()
        Gdx.gl.glPixelStorei(GL20.GL_UNPACK_ALIGNMENT, 1)
        Gdx.gl.glTexSubImage2D(GL20.GL_TEXTURE_2D, 0, 0, 0, frame.width, frame.height,
            GL_BGRA, GL20.GL_UNSIGNED_BYTE, pixels)
    }

    private fun loadEntrypoints(): List<BrowserUiEntrypoint> =
        if (config.browserUiUrl.isNotBlank()) {
            listOf(BrowserUiEntrypoint("configured", "configured", config.browserUiUrl, null))
        } else {
            bundleUiSource.load()
        }

    private fun writeBrowserPage(entries: List<BrowserUiEntrypoint>): String {
        val encodedEntries = bundleUiSource.encode(entries)
        check(ENTRYPOINTS_PLACEHOLDER in RUNTIME_PAGE_TEMPLATE) {
            "Browser UI runtime template is missing $ENTRYPOINTS_PLACEHOLDER"
        }
        val html = RUNTIME_PAGE_TEMPLATE.replace(ENTRYPOINTS_PLACEHOLDER, encodedEntries)
        val directory = pageDirectory ?: Files.createTempDirectory("selene-browser-ui-").also {
            pageDirectory = it
        }
        val page = directory.resolve(BROWSER_PAGE_NAME)
        Files.writeString(page, html, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)
        bundleScheme.setRuntimePage(page)
        return bundleScheme.runtimeUrl
    }

    override fun dispose() {
        if (!initialized && app == null && client == null && browser == null && hostFrame == null &&
            pageDirectory == null) return
        initialized = false
        texture?.dispose()
        texture = null
        region = null
        uploadBuffer = null
        bridge.dispose()
        val currentBrowser = browser
        if (currentBrowser != null) {
            EventQueue.invokeLater { currentBrowser.close(true) }
            if (!awaitShutdown(browserClosed, "browser")) {
                logger.warn("CEF browser did not close within {} seconds", SHUTDOWN_TIMEOUT_SECONDS)
            }
        }
        try {
            EventQueue.invokeAndWait {
                hostFrame?.apply {
                    isVisible = false
                    contentPane.removeAll()
                    dispose()
                }
            }
        } catch (error: Exception) {
            logger.warn("Failed to close the browser UI window cleanly", error)
        }
        client?.dispose()
        app?.dispose()
        if (app != null && CefApp.getState() != CefApp.CefAppState.TERMINATED &&
            !awaitShutdown(appTerminated, "application")) {
            logger.error("CEF application did not terminate within {} seconds", SHUTDOWN_TIMEOUT_SECONDS)
        }
        browser = null
        hostFrame = null
        client = null
        app = null
        deletePageDirectory()
    }

    private fun deletePageDirectory() {
        bundleScheme.setRuntimePage(null)
        val directory = pageDirectory ?: return
        pageDirectory = null
        try {
            Files.deleteIfExists(directory.resolve(BROWSER_PAGE_NAME))
            Files.deleteIfExists(directory)
        } catch (error: Exception) {
            logger.warn("Failed to delete temporary browser UI directory {}", directory, error)
        }
    }

    private fun awaitShutdown(latch: CountDownLatch, component: String): Boolean {
        return try {
            latch.await(SHUTDOWN_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        } catch (_: InterruptedException) {
            Thread.currentThread().interrupt()
            logger.warn("Interrupted while waiting for CEF {} shutdown", component)
            false
        }
    }

    private companion object {
        const val GL_BGRA = 0x80E1
        val POINTER_EVENT_TYPES = setOf("mousemove", "mousedown", "mouseup", "click", "wheel")
        val KEY_NAMES = setOf("Enter", "Escape", "Backspace", "Tab", "Delete", "Home", "End",
            "PageUp", "PageDown", "ArrowLeft", "ArrowRight", "ArrowUp", "ArrowDown", "Shift",
            "Control", "Alt", "Meta")
        const val UI_READY_TIMEOUT_SECONDS = 30L
        const val SHUTDOWN_TIMEOUT_SECONDS = 5L
        const val BROWSER_PAGE_NAME = "index.html"
        const val ENTRYPOINTS_PLACEHOLDER = "__SELENE_UI_ENTRIES__"
        val RUNTIME_PAGE_TEMPLATE = requireNotNull(
            CefBrowserUi::class.java.getResourceAsStream("/ui/cef/runtime.html")
        ) { "Missing browser UI runtime template" }.bufferedReader().use { it.readText() }
    }
}
