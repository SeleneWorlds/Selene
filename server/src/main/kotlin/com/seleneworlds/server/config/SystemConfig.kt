package com.seleneworlds.server.config

data class SystemConfig(
    val heartbeatServer: String = "https://telescope.seleneworlds.com",
    val authBrokerUrl: String = "https://auth.seleneworlds.com",
    val authBrokerIssuer: String = "selene-auth-broker"
)
