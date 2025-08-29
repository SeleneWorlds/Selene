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
import world.selene.client.data.Registries
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
import world.selene.common.network.packet.DisconnectPacket

class ClientPacketHandler(
    private val logger: Logger,
    private val objectMapper: ObjectMapper,
    private val luaManager: LuaManager,
    private val registries: Registries,
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
        when (packet) {
            is NameIdMappingsPacket -> {
                context.enqueueWork {
                    if (packet.mappings.isEmpty()) {
                        val registry = registries.getRegistry(packet.scope)
                        registry?.registryPopulated(nameIdRegistry, false)
                    } else {
                        packet.mappings.forEach { nameIdRegistry.addExisting(packet.scope, it.key, it.value) }
                    }
                }
            }

            is MapChunkPacket -> {
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
            }

            is RemoveMapChunkPacket -> {
                context.enqueueWork {
                    clientMap.removeChunk(packet.x, packet.y, packet.z, packet.width, packet.height)
                }
            }

            is UpdateMapTilesPacket -> {
                context.enqueueWork {
                    clientMap.updateTile(
                        packet.coordinate,
                        packet.baseTileId,
                        packet.additionalTileIds
                    )
                }
            }

            is EntityPacket -> {
                context.enqueueWork {
                    val componentOverrides =
                        packet.components.mapValues {
                            objectMapper.readValue(
                                it.value,
                                ComponentConfiguration::class.java
                            )
                        }
                    clientMap.placeOrUpdateEntity(
                        packet.entityId,
                        packet.networkId,
                        packet.coordinate,
                        packet.facing,
                        componentOverrides
                    )
                }
            }

            is RemoveEntityPacket -> {
                context.enqueueWork {
                    clientMap.removeEntityByNetworkId(packet.networkId)
                }
            }

            is SetCameraPositionPacket -> {
                context.enqueueWork {
                    cameraManager.focusCamera(packet.coordinate)
                }
            }

            is SetCameraFollowEntityPacket -> {
                context.enqueueWork {
                    cameraManager.focusEntity(packet.networkId)
                }
            }

            is SetControlledEntityPacket -> {
                context.enqueueWork {
                    playerController.controlledEntityNetworkId = packet.networkId
                }
            }

            is MoveEntityPacket -> {
                context.enqueueWork {
                    clientMap.getEntityByNetworkId(packet.networkId)?.move(packet.end, packet.duration, packet.facing)
                    if (playerController.controlledEntityNetworkId == packet.networkId) {
                        gridMovement.confirmMove()
                    }
                }
            }

            is PlaySoundPacket -> {
                context.enqueueWork {
                    val sound = registries.sounds.get(packet.soundId)
                    if (sound == null) {
                        logger.warn("Could not play sound with id ${packet.soundId}")
                        return@enqueueWork
                    }
                    soundManager.playSound(sound, packet.volume, packet.pitch)
                }
            }

            is StopSoundPacket -> {
                context.enqueueWork {
                    if (packet.soundId == -1) {
                        soundManager.stopAllSounds()
                    } else {
                        val sound = registries.sounds.get(packet.soundId)
                        if (sound == null) {
                            logger.warn("Could not stop sound with id ${packet.soundId}")
                            return@enqueueWork
                        }
                        soundManager.stopSound(sound)
                    }
                }
            }

            is CustomPayloadPacket -> {
                context.enqueueWork {
                    val handler = payloadRegistry.retrieveHandler(packet.payloadId)
                    if (handler != null) {
                        handler.callback.push(luaManager.lua)
                        val payload = objectMapper.readValue(packet.payload, Map::class.java)
                        luaManager.lua.push(payload)
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

            is DisconnectPacket -> {
                context.disconnect()
                logger.info("Disconnected from server: ${packet.reason}")
            }
        }
    }
}