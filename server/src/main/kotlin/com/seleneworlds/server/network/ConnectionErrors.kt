package com.seleneworlds.server.network

import java.net.SocketException
import java.util.Collections
import java.util.IdentityHashMap

internal fun Throwable.isConnectionReset(): Boolean {
    val visited = Collections.newSetFromMap(IdentityHashMap<Throwable, Boolean>())
    var current: Throwable? = this
    while (current != null && visited.add(current)) {
        if (current is SocketException &&
            (current.message.equals("Connection reset", ignoreCase = true) ||
                current.message.equals("Connection reset by peer", ignoreCase = true))
        ) {
            return true
        }
        current = current.cause
    }
    return false
}
