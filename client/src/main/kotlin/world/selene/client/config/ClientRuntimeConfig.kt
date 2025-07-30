package world.selene.client.config

data class ClientRuntimeConfig(
    val host: String,
    val port: Int,
    val token: String,
    val bundles: Map<String, String>
)