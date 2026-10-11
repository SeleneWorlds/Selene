package com.seleneworlds.server.monitoring

import io.sentry.Sentry
import io.sentry.SentryEvent
import io.sentry.SentryLevel
import io.sentry.log4j2.SentryAppender
import org.apache.logging.log4j.core.LoggerContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertSame
import kotlin.test.assertTrue

class SentryLoggingTest {
    @Test
    fun `server logging captures errors with breadcrumbs and installs uncaught handler`() {
        val events = mutableListOf<SentryEvent>()
        val previousHandler = Thread.getDefaultUncaughtExceptionHandler()
        Sentry.init { options ->
            options.dsn = "https://public@example.invalid/1"
            options.isEnableExternalConfiguration = false
            options.setBeforeSend { event, _ ->
                events.add(event)
                null // Verify capture without sending anything over the network.
            }
        }
        try {
            assertTrue(Thread.getDefaultUncaughtExceptionHandler() !== previousHandler)
            val configuration = javaClass.classLoader.getResource("log4j2.properties")!!
            LoggerContext("sentry-test").use { context ->
                context.configLocation = configuration.toURI()
                val appender = assertIs<SentryAppender>(context.configuration.getAppender("SENTRY"))
                assertTrue(appender.isStarted)
                val logger = context.getLogger("Selene")
                logger.info("Sentry test breadcrumb")
                logger.warn("Sentry test warning")
                assertTrue(events.isEmpty())

                val failure = IllegalStateException("Sentry test failure")
                logger.error("Sentry test error", failure)

                val event = events.single()
                assertEquals(SentryLevel.ERROR, event.level)
                assertSame(failure, event.throwable)
                assertTrue(event.breadcrumbs.orEmpty().any { it.message == "Sentry test breadcrumb" })
            }
        } finally {
            Sentry.close()
        }
    }
}
