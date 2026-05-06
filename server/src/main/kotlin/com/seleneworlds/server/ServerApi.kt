package com.seleneworlds.server

import com.seleneworlds.common.data.Identifier
import com.seleneworlds.common.observable.ObservableMap
import com.seleneworlds.server.data.ServerCustomData

class ServerApi(
    serverCustomData: ServerCustomData
) {
    val customData: ObservableMap = serverCustomData.customData

    fun getCustomData(identifier: Identifier): Any? {
        return customData[identifier.toString()]
    }

    fun getCustomDataMap(identifier: Identifier): ObservableMap {
        val key = identifier.toString()
        val value = customData[key]
        return value as? ObservableMap ?: ObservableMap().also { customData[key] = it }
    }

    fun setCustomData(identifier: Identifier, value: Any?) {
        customData[identifier.toString()] = value
    }
}
