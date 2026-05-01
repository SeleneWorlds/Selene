package world.selene.server.lua

import world.selene.common.lua.Signal
import world.selene.common.observable.ObservableMap
import world.selene.server.data.ServerCustomData

class ServerApi(
    signals: ServerLuaSignals,
    serverCustomData: ServerCustomData
) {
    val serverStarted: Signal = signals.serverStarted
    val serverReloaded: Signal = signals.serverReloaded
    val customData: ObservableMap = serverCustomData.customData
}
