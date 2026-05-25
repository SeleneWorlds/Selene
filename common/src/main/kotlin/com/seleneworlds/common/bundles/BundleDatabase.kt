package com.seleneworlds.common.bundles

class BundleDatabase {
    // All resolved bundles, with preserved dependency order.
    private val _resolvedBundles = mutableListOf<Bundle>()
    private val enabledBundleIds = linkedSetOf<String>()

    val resolvedBundles: List<Bundle>
        get() = _resolvedBundles

    val loadedBundles: List<Bundle>
        get() = enabledBundles

    val enabledBundles: List<Bundle>
        get() = _resolvedBundles.filter { it.manifest.name in enabledBundleIds }

    fun addBundle(bundle: Bundle, enabled: Boolean = true) {
        if (_resolvedBundles.any { it.manifest.name == bundle.manifest.name }) {
            return
        }
        _resolvedBundles.add(bundle)
        if (enabled) {
            enabledBundleIds.add(bundle.manifest.name)
        }
    }

    fun enableBundle(bundle: Bundle) {
        enabledBundleIds.add(bundle.manifest.name)
    }

    fun disableBundle(bundle: Bundle) {
        enabledBundleIds.remove(bundle.manifest.name)
    }

    fun isBundleEnabled(bundle: String): Boolean {
        return bundle in enabledBundleIds
    }

    fun getBundle(bundle: String): Bundle? {
        return _resolvedBundles.find { it.manifest.name == bundle }
    }

    fun getEnabledBundle(bundle: String): Bundle? {
        return enabledBundles.find { it.manifest.name == bundle }
    }

    fun getTransitiveDependents(bundle: String): Set<String> {
        val reverseDependencies = _resolvedBundles
            .groupBy { loadedBundle ->
                loadedBundle.manifest.dependencies.toSet()
            }
            .flatMap { (dependencies, bundles) ->
                dependencies.map { dependency -> dependency to bundles.map { it.manifest.name } }
            }
            .groupBy({ it.first }, { it.second })
            .mapValues { (_, dependents) -> dependents.flatten().toSet() }

        val impactedBundles = mutableSetOf<String>()
        val queue = ArrayDeque<String>()
        queue.addLast(bundle)

        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()
            for (dependent in reverseDependencies[current].orEmpty()) {
                if (impactedBundles.add(dependent)) {
                    queue.addLast(dependent)
                }
            }
        }

        return impactedBundles
    }
}
