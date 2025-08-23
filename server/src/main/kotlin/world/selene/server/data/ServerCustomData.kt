package world.selene.server.data

class ServerCustomData {
    private val customData = mutableMapOf<String, Any>()

    fun getCustomData(key: String, defaultValue: Any? = null): Any? {
        return customData.getOrDefault(key, defaultValue)
    }

    fun setCustomData(key: String, value: Any) {
        customData[key] = value
    }
}
