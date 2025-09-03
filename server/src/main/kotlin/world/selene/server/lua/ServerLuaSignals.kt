package world.selene.server.lua

import world.selene.common.lua.Signal

class ServerLuaSignals {
    /**
     * Fired when the server starts.
     */
    val serverStarted = Signal("serverStarted")

    /**
     * Fired when the server reloads.
     */
    val serverReloaded = Signal("serverReloaded")

    /**
     * Fired when a player queues to join the server.
     */
    val playerQueued = Signal("playerQueued")

    /**
     * Fired when a player leaves the queue.
     */
    val playerDequeued = Signal("playerDequeued")

    /**
     * Fired when a player successfully joins the server.
     */
    val playerJoined = Signal("playerJoined")

    /**
     * Fired when a player disconnects from the server.
     */
    val playerLeft = Signal("playerLeft")

    /**
     * Fired when an entity steps on a tile.
     */
    val entitySteppedOnTile = Signal("entitySteppedOnTile")

    /**
     * Fired when an entity steps off a tile.
     */
    val entitySteppedOffTile = Signal("entitySteppedOffTile")
}