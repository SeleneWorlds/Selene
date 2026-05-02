package world.selene.server

import world.selene.common.observable.ObservableMap
import world.selene.server.data.ServerCustomData

class ServerApi(
    serverCustomData: ServerCustomData
) {
    val customData: ObservableMap = serverCustomData.customData
}