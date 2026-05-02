package com.seleneworlds.common.network

interface PacketHandler<T> {
    fun handle(context: T, packet: Packet)
}