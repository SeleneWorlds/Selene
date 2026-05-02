package world.selene.client.script

interface ClientScriptProvider {
    fun loadEntityScript(module: String): ClientEntityScript
}