package com.seleneworlds.server.network

import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ServerPacketHandlerTest {
    @Test
    fun `parses supported client locales`() {
        assertEquals(Locale.of("en"), ServerPacketHandler.parseClientLocale("en"))
        assertEquals(Locale.of("en", "US"), ServerPacketHandler.parseClientLocale("en_US"))
        assertEquals(Locale.of("de", "DE"), ServerPacketHandler.parseClientLocale("de-DE"))
        assertEquals(Locale.of("es", "419"), ServerPacketHandler.parseClientLocale("es_419"))
        assertEquals(Locale.of("en", "US", "POSIX"), ServerPacketHandler.parseClientLocale("en_US_POSIX"))
    }

    @Test
    fun `rejects malformed client locales`() {
        listOf(
            "",
            "e",
            "en_",
            "en_US_POSIX_EXTRA",
            "en_UnitedStates",
            "../en_US",
            "a".repeat(1_000),
        ).forEach { locale ->
            assertNull(ServerPacketHandler.parseClientLocale(locale), locale)
        }
    }
}
