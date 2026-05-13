package com.seleneworlds.common.bundles

object BundleExecutionContext {
    private val activeBundle = ThreadLocal<Bundle?>()

    val currentBundle: Bundle?
        get() = activeBundle.get()

    fun <T> withBundle(bundle: Bundle, block: () -> T): T {
        val previousBundle = activeBundle.get()
        activeBundle.set(bundle)
        return try {
            block()
        } finally {
            if (previousBundle != null) {
                activeBundle.set(previousBundle)
            } else {
                activeBundle.remove()
            }
        }
    }
}
