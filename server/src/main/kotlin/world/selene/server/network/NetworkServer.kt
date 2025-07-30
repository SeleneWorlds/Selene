package world.selene.server.network

interface NetworkServer {
    fun start(port: Int)
    fun stop()

    fun reportClientError(client: NetworkClient, cause: Throwable)

    val clients: Collection<NetworkClient>
}