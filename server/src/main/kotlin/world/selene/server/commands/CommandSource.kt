package world.selene.server.commands

import world.selene.common.util.Coordinate

interface CommandSource {
    val name: String
    val position: Coordinate
    fun respond(message: String)
}