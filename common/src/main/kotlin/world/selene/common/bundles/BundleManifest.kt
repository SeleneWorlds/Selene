package world.selene.common.bundles

data class BundleManifest(
    val name: String,
    val description: String = name,
    val entrypoints: List<String> = emptyList(),
    val dependencies: List<String> = emptyList(),
    val preloads: Map<String, String> = emptyMap(),
    val transformers: List<String> = emptyList()
)
