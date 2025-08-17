package world.selene.common.network

import world.selene.common.network.packet.AuthenticatePacket
import world.selene.common.network.packet.SetCameraPositionPacket
import world.selene.common.network.packet.CustomPayloadPacket
import world.selene.common.network.packet.DisconnectPacket
import world.selene.common.network.packet.EntityPacket
import world.selene.common.network.packet.MapChunkPacket
import world.selene.common.network.packet.UpdateMapTilesPacket
import world.selene.common.network.packet.MoveEntityPacket
import world.selene.common.network.packet.NameIdMappingsPacket
import world.selene.common.network.packet.PlaySoundPacket
import world.selene.common.network.packet.RemoveEntityPacket
import world.selene.common.network.packet.RemoveMapChunkPacket
import world.selene.common.network.packet.RequestMovePacket
import world.selene.common.network.packet.SetCameraFollowEntityPacket
import world.selene.common.network.packet.SetControlledEntityPacket
import world.selene.common.network.packet.StopSoundPacket

class PacketRegistrations(private val packetFactory: PacketFactory) {
    fun register() {
        packetFactory.registerPacket(
            1,
            AuthenticatePacket::class,
            AuthenticatePacket::encode,
            AuthenticatePacket::decode
        )
        packetFactory.registerPacket(
            2,
            NameIdMappingsPacket::class,
            NameIdMappingsPacket::encode,
            NameIdMappingsPacket.Companion::decode
        )
        packetFactory.registerPacket(
            3,
            MapChunkPacket::class,
            MapChunkPacket::encode,
            MapChunkPacket::decode
        )
        packetFactory.registerPacket(
            4,
            EntityPacket::class,
            EntityPacket::encode,
            EntityPacket.Companion::decode
        )
        packetFactory.registerPacket(
            5,
            SetCameraPositionPacket::class,
            SetCameraPositionPacket.Companion::encode,
            SetCameraPositionPacket.Companion::decode
        )
        packetFactory.registerPacket(
            6,
            SetCameraFollowEntityPacket::class,
            SetCameraFollowEntityPacket.Companion::encode,
            SetCameraFollowEntityPacket.Companion::decode
        )
        packetFactory.registerPacket(
            7,
            SetControlledEntityPacket::class,
            SetControlledEntityPacket.Companion::encode,
            SetControlledEntityPacket.Companion::decode
        )
        packetFactory.registerPacket(
            8,
            RequestMovePacket::class,
            RequestMovePacket::encode,
            RequestMovePacket::decode
        )
        packetFactory.registerPacket(
            9,
            MoveEntityPacket::class,
            MoveEntityPacket::encode,
            MoveEntityPacket::decode
        )
        packetFactory.registerPacket(
            10,
            RemoveEntityPacket::class,
            RemoveEntityPacket::encode,
            RemoveEntityPacket::decode
        )
        packetFactory.registerPacket(
            11,
            RemoveMapChunkPacket::class,
            RemoveMapChunkPacket::encode,
            RemoveMapChunkPacket::decode
        )
        packetFactory.registerPacket(
            12,
            PlaySoundPacket::class,
            PlaySoundPacket::encode,
            PlaySoundPacket::decode
        )
        packetFactory.registerPacket(
            13,
            StopSoundPacket::class,
            StopSoundPacket::encode,
            StopSoundPacket::decode
        )
        packetFactory.registerPacket(
            14,
            UpdateMapTilesPacket::class,
            UpdateMapTilesPacket::encode,
            UpdateMapTilesPacket::decode
        )
        packetFactory.registerPacket(
            254,
            CustomPayloadPacket::class,
            CustomPayloadPacket::encode,
            CustomPayloadPacket::decode
        )
        packetFactory.registerPacket(255, DisconnectPacket::class, DisconnectPacket::encode, DisconnectPacket::decode)
    }
}