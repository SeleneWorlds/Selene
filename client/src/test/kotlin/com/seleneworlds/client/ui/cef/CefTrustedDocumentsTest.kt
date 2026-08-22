package com.seleneworlds.client.ui.cef

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CefTrustedDocumentsTest {
    @Test
    fun `accepts only canonical forms of the runtime document`() {
        assertTrue(CefTrustedDocuments.contains(CefBundleScheme.RUNTIME_URL))
        assertTrue(CefTrustedDocuments.contains(
            "HTTPS://${CefBundleScheme.HOST.uppercase()}:443/runtime/index.html#chat"))

        assertFalse(CefTrustedDocuments.contains(
            "https://${CefBundleScheme.HOST}/bundle/example/index.html"))
        assertFalse(CefTrustedDocuments.contains(
            "https://${CefBundleScheme.HOST}/runtime/index.html?redirect=evil"))
        assertFalse(CefTrustedDocuments.contains("https://evil.invalid/runtime/index.html"))
        assertFalse(CefTrustedDocuments.contains("javascript:alert(1)"))
        assertFalse(CefTrustedDocuments.contains("not a URL"))
    }
}
