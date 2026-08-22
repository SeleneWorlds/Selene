package com.seleneworlds.client.ui.cef

import java.net.URI
import java.util.Locale

internal object CefTrustedDocuments {
    private val allowlist = setOf(canonicalize(CefBundleScheme.RUNTIME_URL))

    fun contains(url: String): Boolean = runCatching { canonicalize(url) }.getOrNull() in allowlist

    private fun canonicalize(url: String): String {
        val parsed = URI(url).normalize()
        require(parsed.scheme.equals("https", ignoreCase = true))
        require(parsed.host != null && parsed.userInfo == null && parsed.query == null)
        val port = when (parsed.port) {
            -1, 443 -> -1
            else -> parsed.port
        }
        return URI(
            "https",
            null,
            parsed.host.lowercase(Locale.ROOT),
            port,
            parsed.rawPath.ifEmpty { "/" },
            null,
            null
        ).toASCIIString()
    }
}
