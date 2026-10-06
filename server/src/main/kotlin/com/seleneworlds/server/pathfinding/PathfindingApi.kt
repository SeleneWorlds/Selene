package com.seleneworlds.server.pathfinding

import com.seleneworlds.common.grid.Coordinate
import com.seleneworlds.common.grid.Direction
import com.seleneworlds.server.entities.EntityApi

class PathfindingApi(
    private val pathfinder: Pathfinder
) {
    fun findPath(
        entity: EntityApi,
        goal: Coordinate,
        searchRadius: Int = Pathfinder.DEFAULT_SEARCH_RADIUS,
        maxExpandedNodes: Int = Pathfinder.DEFAULT_MAX_EXPANDED_NODES,
    ): List<Direction>? {
        return pathfinder.findPath(entity.delegate, goal, searchRadius, maxExpandedNodes)
    }
}
