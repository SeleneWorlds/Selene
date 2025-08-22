package world.selene.client.network

import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.Logger
import party.iroiro.luajava.LuaException
import world.selene.common.data.NameIdRegistry
import world.selene.common.lua.LuaManager
import world.selene.common.lua.LuaPayloadRegistry
import world.selene.common.network.Packet
import world.selene.common.network.PacketHandler
import world.selene.common.network.packet.MapChunkPacket
import world.selene.common.network.packet.UpdateMapTilesPacket
import world.selene.common.network.packet.NameIdMappingsPacket
import world.selene.client.camera.CameraManager
import world.selene.client.controls.GridMovement
import world.selene.client.controls.PlayerController
import world.selene.client.maps.ClientMap
import world.selene.common.network.packet.CustomPayloadPacket
import world.selene.common.network.packet.EntityPacket
import world.selene.common.network.packet.MoveEntityPacket
import world.selene.common.network.packet.PlaySoundPacket
import world.selene.common.network.packet.RemoveEntityPacket
import world.selene.common.network.packet.RemoveMapChunkPacket
import world.selene.common.network.packet.SetCameraFollowEntityPacket
import world.selene.common.network.packet.SetCameraPositionPacket
import world.selene.common.network.packet.SetControlledEntityPacket
import world.selene.common.network.packet.StopSoundPacket
import world.selene.client.sound.SoundManager
import world.selene.common.data.ComponentConfiguration

class ClientPacketHandler(
    private val logger: Logger,
    private val objectMapper: ObjectMapper,
    private val luaManager: LuaManager,
    private val payloadRegistry: LuaPayloadRegistry,
    private val nameIdRegistry: NameIdRegistry,
    private val clientMap: ClientMap,
    private val cameraManager: CameraManager,
    private val playerController: PlayerController,
    private val gridMovement: GridMovement,
    private val soundManager: SoundManager
) : PacketHandler<NetworkClient> {
    override fun handle(
        context: NetworkClient,
        packet: Packet
    ) {
        if (packet is NameIdMappingsPacket) {
            context.enqueueWork {
                packet.mappings.forEach { nameIdRegistry.addExisting(packet.scope, it.key, it.value) }
            }
        } else if (packet is MapChunkPacket) {
            context.enqueueWork {
                clientMap.setChunk(
                    packet.x,
                    packet.y,
                    packet.z,
                    packet.width,
                    packet.height,
                    packet.baseTiles,
                    packet.additionalTiles
                )
            }
        } else if (packet is RemoveMapChunkPacket) {
            context.enqueueWork {
                clientMap.removeChunk(packet.x, packet.y, packet.z, packet.width, packet.height)
            }
        } else if (packet is UpdateMapTilesPacket) {
            context.enqueueWork {
                clientMap.updateTile(
                    packet.coordinate,
                    packet.baseTileId,
                    packet.additionalTileIds
                )
            }
        } else if (packet is EntityPacket) {
            context.enqueueWork {
                val componentOverrides =
                    packet.components.mapValues { objectMapper.readValue(it.value, ComponentConfiguration::class.java) }
                clientMap.placeOrUpdateEntity(
                    packet.entityId,
                    packet.networkId,
                    packet.coordinate,
                    packet.facing,
                    componentOverrides
                )
            }
        } else if (packet is RemoveEntityPacket) {
            context.enqueueWork {
                clientMap.removeEntityByNetworkId(packet.networkId)
            }
        } else if (packet is SetCameraPositionPacket) {
            context.enqueueWork {
                cameraManager.focusCamera(packet.coordinate)
            }
        } else if (packet is SetCameraFollowEntityPacket) {
            context.enqueueWork {
                cameraManager.focusEntity(packet.networkId)
            }
        } else if (packet is SetControlledEntityPacket) {
            context.enqueueWork {
                playerController.controlledEntityNetworkId = packet.networkId
            }
        } else if (packet is MoveEntityPacket) {
            context.enqueueWork {
                clientMap.getEntityByNetworkId(packet.networkId)?.move(packet.end, packet.duration, packet.facing)
                if (playerController.controlledEntityNetworkId == packet.networkId) {
                    gridMovement.confirmMove()
                }
            }
        } else if (packet is PlaySoundPacket) {
            context.enqueueWork {
                soundManager.playSound(packet.soundId, packet.volume, packet.pitch)
            }
        } else if (packet is StopSoundPacket) {
            context.enqueueWork {
                if (packet.soundId == -1) {
                    soundManager.stopAllSounds()
                } else {
                    soundManager.stopSound(packet.soundId)
                }
            }
        } else if (packet is CustomPayloadPacket) {
            context.enqueueWork {
                val handler = payloadRegistry.retrieveHandler(packet.payloadId)
                if (handler != null) {
                    handler.callback.push(luaManager.lua)
                    luaManager.lua.push(packet.payload)
                    try {
                        luaManager.lua.pCall(1, 0)
                    } catch (e: LuaException) {
                        logger.error(
                            "Error while handling custom payload ${packet.payloadId} (registered at ${handler.registrationSite})",
                            e
                        )
                    }
                }
            }
        }
    }
}