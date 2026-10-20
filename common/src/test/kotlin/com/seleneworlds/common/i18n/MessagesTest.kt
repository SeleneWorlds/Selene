package com.seleneworlds.common.i18n

import com.seleneworlds.common.bundles.Bundle
import com.seleneworlds.common.bundles.BundleDatabase
import com.seleneworlds.common.bundles.BundleManifest
import org.slf4j.LoggerFactory
import java.util.Locale
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.createDirectories
import kotlin.io.path.createTempDirectory
import kotlin.io.path.deleteRecursively
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

@OptIn(ExperimentalPathApi::class)
class MessagesTest {
    @Test
    fun `reload refreshes all cached locales and removes deleted translations`() {
        val root = createTempDirectory("selene-messages-reload-test")
        try {
            val directory = root.resolve("server/i18n").createDirectories()
            val german = directory.resolve("messages_de.properties")
            val english = directory.resolve("messages_en.properties")
            german.writeText("greeting=Hallo\nremoved=Alt\n")
            english.writeText("greeting=Hello\n")
            val database = BundleDatabase().apply {
                addBundle(Bundle(BundleManifest(name = "test"), root.toFile()))
            }
            val messages = Messages(database, LoggerFactory.getLogger(MessagesTest::class.java))
            assertEquals("Hallo", messages.get("greeting", Locale.GERMAN))
            assertEquals("Hello", messages.get("greeting", Locale.ENGLISH))
            assertNull(messages.get("added", Locale.GERMAN))

            german.writeText("greeting=Grüße\nadded=Neu\n")
            java.nio.file.Files.delete(english)
            messages.reload()

            assertEquals("Grüße", messages.get("greeting", Locale.GERMAN))
            assertEquals("Neu", messages.get("added", Locale.GERMAN))
            assertNull(messages.get("removed", Locale.GERMAN))
            assertNull(messages.get("greeting", Locale.ENGLISH))
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun `UTF-8 translations preserve umlauts and properties escapes`() {
        val root = createTempDirectory("selene-messages-test")
        try {
            val directory = root.resolve("client/i18n").createDirectories()
            directory.resolve("messages_de.properties").writeText(
                "menu.title=Menü\ncharacters=ÄÖÜ äöü ß\nescaped=Men\\u00fc\n",
                Charsets.UTF_8
            )
            val database = BundleDatabase().apply {
                addBundle(Bundle(BundleManifest(name = "test"), root.toFile()))
            }
            val messages = Messages(database, LoggerFactory.getLogger(MessagesTest::class.java))

            assertEquals("Menü", messages.get("menu.title", Locale.GERMAN))
            assertEquals("ÄÖÜ äöü ß", messages.get("characters", Locale.GERMAN))
            assertEquals("Menü", messages.get("escaped", Locale.GERMAN))
        } finally {
            root.deleteRecursively()
        }
    }
}
