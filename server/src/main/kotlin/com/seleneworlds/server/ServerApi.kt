package com.seleneworlds.server

import com.seleneworlds.common.data.Identifier
import com.seleneworlds.common.observable.ObservableMap
import com.seleneworlds.server.data.ServerCustomData

class ServerApi(
    serverCustomData: com.seleneworlds.server.data.ServerCustomData
) {
    val customData: ObservableMap = serverCustomData.customData

    fun getCustomData(identifier: Identifier): Any? {
        return customData[identifier.toString()]
    }

    fun setCustomData(identifier: Identifier, value: Any?) {
        customData[identifier.toString()] = value
    }
}
