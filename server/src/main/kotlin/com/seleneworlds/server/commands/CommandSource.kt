package com.seleneworlds.server.commands

import com.seleneworlds.common.grid.Coordinate

interface CommandSource {
    val name: String
    val position: Coordinate
    fun respond(message: String)
}