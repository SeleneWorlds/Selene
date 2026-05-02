package com.seleneworlds.server

import com.seleneworlds.common.observable.ObservableMap
import com.seleneworlds.server.data.ServerCustomData

class ServerApi(
    serverCustomData: com.seleneworlds.server.data.ServerCustomData
) {
    val customData: ObservableMap = serverCustomData.customData
}