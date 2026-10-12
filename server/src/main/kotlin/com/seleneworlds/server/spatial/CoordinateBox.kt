package com.seleneworlds.server.spatial

import com.seleneworlds.common.grid.Coordinate
import kotlin.math.abs

/** Inclusive axis-aligned box; half-extents are independent distances on each axis. */
internal class CoordinateBox(val center: Coordinate, val halfExtents: Coordinate) {
    init {
        require(halfExtents.x >= 0 && halfExtents.y >= 0 && halfExtents.z >= 0) {
            "Box half-extents must be non-negative"
        }
    }

    fun contains(position: Coordinate): Boolean =
        abs(position.x.toLong() - center.x) <= halfExtents.x &&
            abs(position.y.toLong() - center.y) <= halfExtents.y &&
            abs(position.z.toLong() - center.z) <= halfExtents.z
}
