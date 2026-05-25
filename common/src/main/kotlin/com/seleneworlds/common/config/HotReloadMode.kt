package com.seleneworlds.common.config

enum class HotReloadMode {
    OFF,
    ON,
    EAGER;

    val isEnabled: Boolean
        get() = this != OFF

    companion object {
        fun parse(value: String): HotReloadMode {
            return when (value.trim().lowercase()) {
                "false", "off", "disabled", "no", "0" -> OFF
                "true", "on", "enabled", "yes", "1" -> ON
                "eager" -> EAGER
                else -> throw IllegalArgumentException("Unknown hot reload mode: $value")
            }
        }
    }
}
