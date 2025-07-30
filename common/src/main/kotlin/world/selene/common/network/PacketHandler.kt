package world.selene.common.network

interface PacketHandler<T> {
    fun handle(context: T, packet: Packet)
}