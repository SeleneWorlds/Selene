package com.seleneworlds.common.jobs

import com.seleneworlds.common.bundles.Bundle
import com.seleneworlds.common.bundles.BundleExecutionContext
import com.seleneworlds.common.bundles.BundleManifest
import com.seleneworlds.common.threading.MainThreadDispatcher
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals

class SchedulesApiTest {

    @Test
    fun `clearBundleState cancels only matching bundle schedules`() {
        val dispatcher = MainThreadDispatcher().apply { bindToCurrentThread() }
        val schedulesApi = SchedulesApi(dispatcher)
        val bundleA = Bundle(BundleManifest(name = "bundle-a"), Files.createTempDirectory("schedules-bundle-a").toFile())
        val bundleB = Bundle(BundleManifest(name = "bundle-b"), Files.createTempDirectory("schedules-bundle-b").toFile())
        var bundleATimeoutCalls = 0
        var bundleAIntervalCalls = 0
        var bundleBTimeoutCalls = 0
        var bundleBIntervalCalls = 0

        try {
            BundleExecutionContext.withBundle(bundleA) {
                schedulesApi.setTimeout(50) { bundleATimeoutCalls++ }
                schedulesApi.setInterval(50) { bundleAIntervalCalls++ }
            }
            BundleExecutionContext.withBundle(bundleB) {
                schedulesApi.setTimeout(50) { bundleBTimeoutCalls++ }
                schedulesApi.setInterval(50) { bundleBIntervalCalls++ }
            }

            schedulesApi.clearBundleState(bundleA)

            Thread.sleep(140)
            dispatcher.process()

            assertEquals(0, bundleATimeoutCalls)
            assertEquals(0, bundleAIntervalCalls)
            assertEquals(1, bundleBTimeoutCalls)
            assertEquals(true, bundleBIntervalCalls > 0)
        } finally {
            schedulesApi.dispose()
            bundleA.dir.deleteRecursively()
            bundleB.dir.deleteRecursively()
        }
    }
}
