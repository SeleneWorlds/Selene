package com.seleneworlds.server.entities

import com.seleneworlds.common.grid.Coordinate
import com.seleneworlds.server.dimensions.Dimension

/** Main-thread index. Horizontal queries deliberately include every floor. */
internal class EntitySpatialIndex {
    private data class Cell(val dimension: Dimension?, val x: Int, val y: Int)
    private class Entry(val entity: Entity, val order: Long, var cell: Cell)
    private val entries = mutableMapOf<Int, Entry>()
    private val cells = mutableMapOf<Cell, MutableSet<Entry>>()
    private var nextOrder = 0L
    private var extremeCoordinates = 0

    private fun cell(entity: Entity) = Cell(
        entity.dimension, Math.floorDiv(entity.coordinate.x, CELL_SIZE),
        Math.floorDiv(entity.coordinate.y, CELL_SIZE)
    )

    // Outside these bounds the original Int subtraction can wrap. Keep its semantics
    // with a full scan rather than silently dropping candidates at extreme coordinates.
    private fun extreme(cell: Cell) = cell.x !in -SAFE_CELL until SAFE_CELL ||
        cell.y !in -SAFE_CELL until SAFE_CELL

    fun add(entity: Entity) {
        val previous = entries[entity.networkId]
        if (previous != null) remove(entity.networkId)
        val entry = Entry(entity, previous?.order ?: nextOrder++, cell(entity))
        entries[entity.networkId] = entry
        insert(entry)
    }

    fun remove(id: Int) {
        val entry = entries.remove(id) ?: return
        erase(entry)
    }

    fun update(entity: Entity) {
        val entry = entries[entity.networkId] ?: return
        if (entry.entity !== entity) return // Unregistered/transient objects are not indexed.
        val destination = cell(entity)
        if (entry.cell == destination) return
        erase(entry)
        entry.cell = destination
        insert(entry)
    }

    private fun insert(entry: Entry) {
        cells.getOrPut(entry.cell) { linkedSetOf() }.add(entry)
        if (extreme(entry.cell)) extremeCoordinates++
    }

    private fun erase(entry: Entry) {
        cells[entry.cell]?.let { bucket ->
            bucket.remove(entry)
            if (bucket.isEmpty()) cells.remove(entry.cell)
        }
        if (extreme(entry.cell)) extremeCoordinates--
    }

    fun nearby(center: Coordinate, dimension: Dimension?, radius: Int): List<Entity> {
        if (radius < 0) return emptyList()
        if (extremeCoordinates > 0 || radius >= SAFE_COORDINATE ||
            center.x !in -SAFE_COORDINATE until SAFE_COORDINATE ||
            center.y !in -SAFE_COORDINATE until SAFE_COORDINATE) {
            return ordered(entries.values.filter {
                it.entity.dimension == dimension && it.entity.coordinate.horizontalDistanceTo(center) <= radius
            })
        }
        // Distance truncates to Int and uses Float arithmetic. Include its rounding fringe.
        val extent = radius.toLong() + radius / 8_388_608 + 2
        val minX = Math.floorDiv(center.x.toLong() - extent, CELL_SIZE.toLong()).toInt()
        val maxX = Math.floorDiv(center.x.toLong() + extent, CELL_SIZE.toLong()).toInt()
        val minY = Math.floorDiv(center.y.toLong() - extent, CELL_SIZE.toLong()).toInt()
        val maxY = Math.floorDiv(center.y.toLong() + extent, CELL_SIZE.toLong()).toInt()
        val cellCount = (maxX.toLong() - minX + 1) * (maxY.toLong() - minY + 1)
        val candidates = if (cellCount > entries.size) {
            entries.values.asSequence()
        } else {
            sequence {
                for (x in minX..maxX) for (y in minY..maxY) {
                    cells[Cell(dimension, x, y)]?.let { yieldAll(it) }
                }
            }
        }
        return ordered(candidates.filter {
            it.entity.dimension == dimension && it.entity.coordinate.horizontalDistanceTo(center) <= radius
        }.toList())
    }

    fun at(coordinate: Coordinate, dimension: Dimension?): List<Entity> = ordered(
        cells[Cell(dimension, Math.floorDiv(coordinate.x, CELL_SIZE), Math.floorDiv(coordinate.y, CELL_SIZE))]
            .orEmpty().filter { it.entity.coordinate == coordinate }
    )

    private fun ordered(entries: Collection<Entry>): List<Entity> =
        entries.sortedBy { it.order }.map { it.entity }

    companion object {
        private const val CELL_SIZE = 16
        private const val SAFE_COORDINATE = 1 shl 29
        private const val SAFE_CELL = SAFE_COORDINATE / CELL_SIZE
    }
}
