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
        val html = """
            <!doctype html><html><head><meta charset="utf-8">
            <meta name="viewport" content="width=device-width,initial-scale=1">
            <style>
              html,body,#bundle-ui{position:absolute;inset:0;margin:0;background:transparent;overflow:hidden}
              .bundle-ui-host{position:absolute;inset:0;pointer-events:none}
            </style></head><body><div id="bundle-ui"></div><script type="module">
            const entries = $encodedEntries;
            const mounted = [];
            const payloadCallbacks = new Map();
            let nextSubscriptionId = 1;
            const network = Object.freeze({
              sendToServer(payloadId, payload = {}) {
                window.seleneQuery({request:JSON.stringify({type:'send',payloadId,payload}),
                  onFailure:(code,message)=>console.error('sendToServer failed',code,message)});
              },
              onPayload(payloadId, callback) {
                const subscriptionId = String(nextSubscriptionId++);
                payloadCallbacks.set(subscriptionId,callback);
                const queryId = window.seleneQuery({request:JSON.stringify({type:'subscribe',payloadId,subscriptionId}),persistent:true,
                  onFailure:(code,message)=>console.error('onPayload failed',code,message)});
                return () => {
                  payloadCallbacks.delete(subscriptionId);
                  window.seleneQueryCancel(queryId);
                };
              }
            });
            const selene = Object.freeze({apiVersion:1,network});

            function rewriteRelativeUrls(root, baseUrl) {
              for (const element of root.querySelectorAll('[src],[href]')) {
                for (const attribute of ['src','href']) {
                  const value = element.getAttribute(attribute);
                  if (value && !value.startsWith('#')) element.setAttribute(attribute,new URL(value,baseUrl).href);
                }
              }
            }

            async function mountEntry(entry) {
              const source = entry.html ?? await fetch(entry.url).then(response => {
                if (!response.ok) throw new Error(`Failed to fetch ${'$'}{entry.bundle}:${'$'}{entry.id}: ${'$'}{response.status}`);
                return response.text();
              });
              const parsed = new DOMParser().parseFromString(source,'text/html');
              const host = document.createElement('div');
              host.className = 'bundle-ui-host';
              host.dataset.bundle = entry.bundle;
              host.dataset.entrypoint = entry.id;
              const root = host.attachShadow({mode:'closed'});
              document.querySelector('#bundle-ui').append(host);
              for (const style of parsed.querySelectorAll('style')) root.append(style.cloneNode(true));
              for (const link of parsed.querySelectorAll('link[rel="stylesheet"][href]')) {
                const copy = document.createElement('link');
                copy.rel = 'stylesheet';
                copy.href = new URL(link.getAttribute('href'),entry.url).href;
                root.append(copy);
              }
              const content = document.createDocumentFragment();
              for (const child of [...parsed.body.childNodes]) content.append(child.cloneNode(true));
              rewriteRelativeUrls(content,entry.url);
              root.append(content);
              const disposers = [];
              for (const script of parsed.querySelectorAll('script[type="module"][src]')) {
                const module = await import(new URL(script.getAttribute('src'),entry.url).href);
                if (typeof module.mount !== 'function') throw new Error(`${'$'}{entry.bundle}:${'$'}{entry.id} must export mount(root,selene)`);
                const dispose = await module.mount(root,selene);
                if (typeof dispose === 'function') disposers.push(dispose);
              }
              mounted.push({host,root,disposers});
            }

            let previousInteraction = '';
            const interactiveElements = new Set();
            let interactionFrame = 0;

            function scheduleInteraction() {
              if (interactionFrame) return;
              interactionFrame = requestAnimationFrame(() => {
                interactionFrame = 0;
                reportInteraction();
              });
            }

            const resizeObserver = new ResizeObserver(scheduleInteraction);
            function refreshInteractiveElements() {
              const current = new Set();
              for (const ui of mounted)
                for (const element of ui.root.querySelectorAll('[data-selene-interactive]')) current.add(element);
              resizeObserver.disconnect();
              interactiveElements.clear();
              for (const ui of mounted) resizeObserver.observe(ui.host);
              for (const element of current) {
                interactiveElements.add(element);
                resizeObserver.observe(element);
              }
            }

            function observeInteraction(ui) {
              new MutationObserver(() => {
                refreshInteractiveElements();
                scheduleInteraction();
              }).observe(ui.root,{subtree:true,childList:true,characterData:true,attributes:true});
              ui.root.addEventListener('focusin',scheduleInteraction,true);
              ui.root.addEventListener('focusout',scheduleInteraction,true);
              ui.root.addEventListener('scroll',scheduleInteraction,true);
              refreshInteractiveElements();
            }

            function reportInteraction() {
              const regions = [];
              let editableFocused = false;
              for (const ui of mounted) {
                const active = ui.root.activeElement;
                editableFocused ||= active instanceof HTMLInputElement || active instanceof HTMLTextAreaElement ||
                  active instanceof HTMLSelectElement || active?.isContentEditable === true;
              }
              for (const element of interactiveElements) {
                const style = getComputedStyle(element);
                if (style.pointerEvents === 'none' || style.visibility === 'hidden' || style.display === 'none') continue;
                const rect = element.getBoundingClientRect();
                if (rect.width <= 0 || rect.height <= 0) continue;
                regions.push({x:Math.floor(rect.x),y:Math.floor(rect.y),
                  width:Math.ceil(rect.width),height:Math.ceil(rect.height)});
              }
              const interaction = JSON.stringify({type:'interaction',regions,editableFocused});
              if (interaction === previousInteraction) return;
              previousInteraction = interaction;
              window.seleneQuery({request:interaction,onFailure:(code,message)=>
                console.error('interaction update failed',code,message)});
            }

            let pointerTarget = null;
            function elementAt(x,y) {
              for (let index=mounted.length-1;index>=0;index--) {
                const target = mounted[index].root.elementFromPoint(x,y);
                if (target) return target;
              }
              return null;
            }
            window.__seleneInput = Object.freeze({
              pointer(type,x,y,button,buttons,deltaX,deltaY) {
                let target = type === 'mousedown' ? elementAt(x,y) : pointerTarget ?? elementAt(x,y);
                if (!target) return;
                if (type === 'mousedown') {
                  pointerTarget = target;
                  if (typeof target.focus === 'function') target.focus({preventScroll:true});
                }
                if (type === 'click') {
                  if (target === elementAt(x,y) && typeof target.click === 'function') target.click();
                  pointerTarget = null;
                  return;
                }
                const event = type === 'wheel'
                  ? new WheelEvent(type,{bubbles:true,cancelable:true,clientX:x,clientY:y,deltaX,deltaY})
                  : new MouseEvent(type,{bubbles:true,cancelable:true,clientX:x,clientY:y,button,buttons});
                target.dispatchEvent(event);
                if (type === 'wheel' && !event.defaultPrevented) {
                  let scrollable = target;
                  while (scrollable && scrollable !== document.body) {
                    if (scrollable.scrollHeight > scrollable.clientHeight || scrollable.scrollWidth > scrollable.clientWidth) break;
                    scrollable = scrollable.parentElement;
                  }
                  scrollable?.scrollBy(deltaX,deltaY);
                }
                if (type === 'mouseup') pointerTarget = null;
              },
              keyboard(type,key,shiftKey,ctrlKey,altKey,metaKey) {
                let target = null;
                for (let index=mounted.length-1;index>=0 && !target;index--)
                  target = mounted[index].root.activeElement;
                target ??= document.activeElement ?? window;
                const event = new KeyboardEvent(type,{key,bubbles:true,cancelable:true,composed:true,
                  shiftKey,ctrlKey,altKey,metaKey});
                target.dispatchEvent(event);
                if (type !== 'keydown' || event.defaultPrevented) return;
                if (key === 'Enter') document.execCommand('insertLineBreak');
                else if (key === 'Backspace') document.execCommand('delete');
                else if (key === 'Delete') document.execCommand('forwardDelete');
              },
              payload(subscriptionId,payload) {
                const callback = payloadCallbacks.get(subscriptionId);
                if (!callback) return;
                try { callback(JSON.parse(payload)); }
                catch (error) { console.error('Payload callback failed',subscriptionId,error); }
              }
            });

            Promise.all(entries.map(mountEntry)).then(() => {
              console.info(`Mounted ${'$'}{entries.length} bundle UI(s)`);
              for (const ui of mounted) observeInteraction(ui);
              window.addEventListener('resize',scheduleInteraction);
              scheduleInteraction();
            })
              .catch(error => {
                console.error('Bundle UI mounting failed',error);
                scheduleInteraction();
              });
            </script></body></html>
        """.trimIndent()
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
    }
}
