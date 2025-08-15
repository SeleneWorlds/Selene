package world.selene.server.network

import party.iroiro.luajava.Lua
import world.selene.common.data.NameIdRegistry
import world.selene.common.network.Packet
import world.selene.common.network.PacketHandler
import world.selene.common.network.packet.AuthenticatePacket
import world.selene.common.network.packet.MoveEntityPacket
import world.selene.common.network.packet.NameIdMappingsPacket
import world.selene.common.network.packet.RequestMovePacket
import world.selene.server.lua.ServerLuaSignals

class ServerPacketHandler(private val signals: ServerLuaSignals, private val nameIdRegistry: NameIdRegistry) :
    PacketHandler<NetworkClient> {
    override fun handle(
        context: NetworkClient,
        packet: Packet
    ) {
        if (packet is AuthenticatePacket) {
            signals.playerJoined.emit { lua ->
                lua.push((context as NetworkClientImpl).player.luaProxy, Lua.Conversion.NONE)
                1
            }
            for (scope in nameIdRegistry.mappings.rowKeySet()) {
                val mappings = nameIdRegistry.mappings.row(scope)
                mappings.entries.windowed(500, partialWindows = true).forEach { chunk ->
                    context.send(NameIdMappingsPacket(scope, chunk))
                }
            }
        } else if (packet is RequestMovePacket) {
            val controlledEntity = (context as NetworkClientImpl).player.controlledEntity ?: return
            if (!controlledEntity.moveTo(packet.coordinate)) {
                context.send(
                    MoveEntityPacket(
                        controlledEntity.networkId,
                        controlledEntity.coordinate,
                        controlledEntity.coordinate,
                        controlledEntity.facing?.angle ?: 0f,
                        0f
                    )
                )
            }
        }
    }
}