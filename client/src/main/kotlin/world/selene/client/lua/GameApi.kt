package world.selene.client.lua

import world.selene.common.lua.Signal

class GameApi(signals: ClientLuaSignals) {
    val gamePreTick: Signal = signals.gamePreTick
}
