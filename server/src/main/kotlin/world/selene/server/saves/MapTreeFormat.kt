package world.selene.server.saves

import world.selene.server.maps.tree.MapTree
import java.io.File

interface MapTreeFormat {
    fun load(file: File): MapTree
    fun saveFullyInline(file: File, mapTree: MapTree)
}