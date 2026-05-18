package com.seleneworlds.server.config

data class SystemConfig(
    val heartbeatServer: String = "https://telescope.seleneworlds.com",
    val authBrokerIssuer: String = "https://auth-broker.seleneworlds.com",
    val authBrokerJwksUrl: String = "https://auth-broker.seleneworlds.com/.well-known/jwks.json"
)