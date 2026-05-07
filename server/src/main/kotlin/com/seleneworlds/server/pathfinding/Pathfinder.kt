package com.seleneworlds.server.pathfinding

import com.seleneworlds.common.grid.Coordinate
import com.seleneworlds.common.grid.Direction
import com.seleneworlds.server.entities.Entity
import java.util.PriorityQueue
import kotlin.math.abs

class Pathfinder {
    fun findPath(entity: Entity, goal: Coordinate, searchRadius: Int = DEFAULT_SEARCH_RADIUS): List<Direction>? {
        val start = entity.coordinate
        if (start == goal) {
            return emptyList()
        }

        val dimension = entity.dimension ?: return null
        val collisionViewer = entity.collisionViewer
        val world = entity.world
        val directions = world.grid.directions.values
            .asSequence()
            .filter { it.vector != Coordinate.Zero }
            .sortedWith(compareBy(Direction::angle, Direction::name))
            .toList()
        if (directions.isEmpty()) {
            return null
        }

        val cameFrom = mutableMapOf<Coordinate, PathStep>()
        val gScore = mutableMapOf(start to 0)
        val open = PriorityQueue<SearchNode>(compareBy<SearchNode>({ it.fScore }, { it.sequence }))
        var sequence = 0L
        open += SearchNode(start, heuristic(start, goal), 0, sequence++)

        while (open.isNotEmpty()) {
            val current = open.remove()
            if (current.cost != gScore[current.coordinate]) {
                continue
            }

            if (current.coordinate == goal) {
                return reconstructPath(cameFrom, current.coordinate)
            }

            for (direction in directions) {
                val candidate = current.coordinate + direction.vector
                if (heuristic(start, candidate) > searchRadius) {
                    continue
                }
                if (candidate != goal && world.collisionResolver.collidesAt(dimension, collisionViewer, candidate)) {
                    continue
                }

                val tentativeCost = current.cost + 1
                if (tentativeCost >= gScore.getOrDefault(candidate, Int.MAX_VALUE)) {
                    continue
                }

                cameFrom[candidate] = PathStep(current.coordinate, direction)
                gScore[candidate] = tentativeCost
                open += SearchNode(candidate, tentativeCost + heuristic(candidate, goal), tentativeCost, sequence++)
            }
        }

        return null
    }

    private fun reconstructPath(cameFrom: Map<Coordinate, PathStep>, goal: Coordinate): List<Direction> {
        val path = ArrayDeque<Direction>()
        var current = goal
        while (true) {
            val step = cameFrom[current] ?: break
            path.addFirst(step.direction)
            current = step.parent
        }
        return path.toList()
    }

    private fun heuristic(a: Coordinate, b: Coordinate): Int {
        return maxOf(abs(a.x - b.x), abs(a.y - b.y), abs(a.z - b.z))
    }

    private data class SearchNode(
        val coordinate: Coordinate,
        val fScore: Int,
        val cost: Int,
        val sequence: Long
    )

    private data class PathStep(
        val parent: Coordinate,
        val direction: Direction
    )

    companion object {
        const val DEFAULT_SEARCH_RADIUS = 32
    }
}
