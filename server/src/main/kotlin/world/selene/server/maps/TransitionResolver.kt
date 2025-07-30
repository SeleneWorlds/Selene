package world.selene.server.maps

import com.google.common.collect.HashMultiset
import com.google.common.collect.Multisets
import world.selene.common.util.Coordinate
import world.selene.server.data.Registries
import world.selene.server.sync.ScopedChunkView

class TransitionResolver(private val registries: Registries) {
    fun applyTransitions(view: ScopedChunkView) {
        val window = view.window
        val surrounding = arrayOfNulls<Int>(8)
        for (x in 0 until window.width) {
            for (y in 0 until window.height) {
                val tileCounts = HashMultiset.create<Int>(8)
                var i = 0
                for (dy in -1..1) {
                    for (dx in -1..1) {
                        if (dx == 0 && dy == 0) {
                            continue
                        }

                        val ox = x + dx
                        val oy = y + dy
                        val neighbourTileId = view.getBaseTileAtRelative(ox, oy)
                        surrounding[i] = neighbourTileId
                        tileCounts.add(neighbourTileId)
                        i++
                    }
                }

                val centerTileId = view.getBaseTileAtRelative(x, y)
                val centerTileName = registries.mappings.getName("tiles", centerTileId)
                val centerTransition = centerTileName?.let { registries.transitions.get(it) }
                val sortedTileCounts = Multisets.copyHighestCountFirst(tileCounts)
                val transition = sortedTileCounts.entrySet().mapNotNull { entry ->
                    val tileId = entry.element
                    val tileName = registries.mappings.getName("tiles", tileId) ?: return@mapNotNull null
                    var mask = 0
                    for (i in 0 until 8) {
                        if (surrounding[i] == tileId) {
                            mask = mask or (1 shl i)
                        }
                    }
                    val transitions = registries.transitions.get(tileName)
                    val transitionTile = transitions?.findTransitionTile(mask)
                    transitionTile?.let { Pair(transitions, transitionTile) }
                }.filter { it.first.priority > (centerTransition?.priority ?: 0) }.maxByOrNull { it.first.priority }
                if (transition != null) {
                    val transitionTileId = registries.mappings.getId("tiles", transition.second)
                    if (transitionTileId != null) {
                        view.addAdditionalTileFirst(Coordinate(window.x + x, window.y + y, window.z), transitionTileId)
                    }
                }
            }
        }
    }
}