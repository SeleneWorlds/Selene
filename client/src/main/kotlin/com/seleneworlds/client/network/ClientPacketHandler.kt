package com.seleneworlds.client.network

import kotlinx.serialization.json.Json
import org.slf4j.Logger
import com.seleneworlds.client.assets.RuntimeBundleUpdateManager
import com.seleneworlds.client.camera.CameraManager
import com.seleneworlds.client.controls.GridMovement
import com.seleneworlds.client.controls.PlayerController
import com.seleneworlds.client.data.Registries
import com.seleneworlds.client.maps.ClientMap
import com.seleneworlds.client.sounds.SoundManager
import com.seleneworlds.common.entities.ComponentConfiguration
import com.seleneworlds.common.data.mappings.NameIdRegistry
import com.seleneworlds.common.network.PayloadHandlerRegistry
import com.seleneworlds.common.network.Packet
import com.seleneworlds.common.network.PacketHandler
import com.seleneworlds.common.network.packet.*
import com.seleneworlds.common.serialization.SerializedMapSerializer

class ClientPacketHandler(
    private val logger: Logger,
    private val json: Json,
    private val registries: Registries,
    private val payloadRegistry: PayloadHandlerRegistry<Unit>,
    private val nameIdRegistry: NameIdRegistry,
    private val clientMap: ClientMap,
    private val cameraManager: CameraManager,
    private val playerController: PlayerController,
    private val gridMovement: GridMovement,
    private val soundManager: SoundManager,
    private val runtimeBundleUpdateManager: RuntimeBundleUpdateManager
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
                            json.decodeFromString<ComponentConfiguration>(it.value)
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

            is TurnEntityPacket -> {
                context.enqueueWork {
                    clientMap.getEntityByNetworkId(packet.networkId)?.turnTo(packet.facing)
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
                    val handler = payloadRegistry.getHandler(packet.payloadId)
                    if (handler != null) {
                        val payload = json.decodeFromString(SerializedMapSerializer, packet.payload)
                        handler(Unit, payload)
                    }
                }
            }

            is NotifyBundleUpdatePacket -> {
                context.enqueueWork {
                    handleRegistryUpdatePacket(packet)
                }
            }

            is DisconnectPacket -> {
                context.disconnect()
                logger.info("Disconnected from server: ${packet.reason}")
            }
        }
    }
    
    private fun handleRegistryUpdatePacket(packet: NotifyBundleUpdatePacket) {
        runtimeBundleUpdateManager.handleBundleContentUpdate(packet)
    }

}
