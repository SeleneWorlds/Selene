package world.selene.client.config

data class ClientRuntimeConfig(
    val host: String,
    val port: Int,
    val contentServerUrl: String,
    val token: String,
    val bundles: Map<String, String>
)