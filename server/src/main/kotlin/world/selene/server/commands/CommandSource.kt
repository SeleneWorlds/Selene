package world.selene.server.commands

import world.selene.common.grid.Coordinate

interface CommandSource {
    val name: String
    val position: Coordinate
    fun respond(message: String)
}