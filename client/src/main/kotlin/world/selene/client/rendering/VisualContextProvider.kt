package world.selene.client.rendering

import world.selene.client.old.VisualContext
import world.selene.common.util.Coordinate

interface VisualContextProvider {
    fun getVisualContext(coordinate: Coordinate): VisualContext
}