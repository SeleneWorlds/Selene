package world.selene.common.tiles.transitions

import com.fasterxml.jackson.annotation.JsonIgnore

data class TransitionDefinition(
    val priority: Int,
    val transitions: List<Entry> = emptyList()
) {
    fun findTransitionTile(mask: Int): String? {
        return transitions.firstOrNull { it.neighbourMask == mask }?.tile
    }

    data class Entry(val tile: String, val neighbours: List<String>) {
        @get:JsonIgnore
        val neighbourMask: Int

        init {
            var mask = 0
            if (neighbours[0][0] == '1') mask = mask or 1
            if (neighbours[0][1] == '1') mask = mask or 2
            if (neighbours[0][2] == '1') mask = mask or 4
            if (neighbours[1][0] == '1') mask = mask or 8
            if (neighbours[1][2] == '1') mask = mask or 16
            if (neighbours[2][0] == '1') mask = mask or 32
            if (neighbours[2][1] == '1') mask = mask or 64
            if (neighbours[2][2] == '1') mask = mask or 128
            neighbourMask = mask
        }
    }
}