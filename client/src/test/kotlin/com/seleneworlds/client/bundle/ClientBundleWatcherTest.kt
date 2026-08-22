package com.seleneworlds.client.bundle

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ClientBundleWatcherTest {
    @Test
    fun `browser UI reloads for manifest and client file changes`() {
        assertTrue(ClientBundleWatcher.isBrowserUiFile("bundle.json"))
        assertTrue(ClientBundleWatcher.isBrowserUiFile("client/ui/dist/ui.js"))
        assertTrue(ClientBundleWatcher.isBrowserUiFile("client\\ui\\dist\\index.html"))
    }

    @Test
    fun `server and common changes do not reload browser UI`() {
        assertFalse(ClientBundleWatcher.isBrowserUiFile("server/lua/chat.lua"))
        assertFalse(ClientBundleWatcher.isBrowserUiFile("common/data/items.json"))
    }

    @Test
    fun `non-ui client changes do not reload browser UI`() {
        assertFalse(ClientBundleWatcher.isBrowserUiFile("client/lua/chat.lua"))
    }
}
