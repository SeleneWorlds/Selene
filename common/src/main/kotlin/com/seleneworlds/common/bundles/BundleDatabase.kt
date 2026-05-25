package com.seleneworlds.common.bundles

class BundleDatabase {
    // All loaded bundles, with preserved order
    val loadedBundles = mutableListOf<Bundle>()

    fun addBundle(bundle: Bundle) {
        loadedBundles.add(bundle)
    }

    fun getBundle(bundle: String): Bundle? {
        return loadedBundles.find { it.manifest.name == bundle }
    }

    fun getTransitiveDependents(bundle: String): Set<String> {
        val reverseDependencies = loadedBundles
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
