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

@OptIn(ExperimentalPathApi::class)
class MessagesTest {
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
