package com.seleneworlds.common.threading

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame
import kotlin.test.assertTrue
import java.util.concurrent.CompletionException

class AwaitableTest {

    @Test
    fun `completed helper returns resolved awaitable`() {
        val awaitable = Awaitable.completed("done")

        assertTrue(awaitable.isDone)
        assertEquals("done", awaitable.join())
    }

    @Test
    fun `failed helper returns rejected awaitable`() {
        val error = IllegalStateException("boom")
        val awaitable = Awaitable.failed<String>(error)

        val thrown = assertFailsWith<CompletionException> {
            awaitable.join()
        }
        assertSame(error, thrown.cause)
    }
}
