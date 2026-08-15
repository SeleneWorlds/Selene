package com.seleneworlds.server.network

import io.netty.util.AttributeKey

object NetworkClientAttributes {
    val CLIENT: AttributeKey<NetworkClient> = AttributeKey.valueOf("selene:client")
}
