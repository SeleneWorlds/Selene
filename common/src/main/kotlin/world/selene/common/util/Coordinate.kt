package world.selene.common.util

import party.iroiro.luajava.Lua
import world.selene.common.lua.LuaMappedMetatable
import world.selene.common.lua.LuaMetatable
import world.selene.common.lua.LuaMetatableProvider
import world.selene.common.lua.checkCoordinate
import world.selene.common.lua.checkUserdata
import kotlin.math.sqrt

data class Coordinate(val x: Int, val y: Int, val z: Int) : LuaMetatableProvider {
    fun horizontalDistanceTo(other: Coordinate): Int {
        val dx = (x - other.x).toFloat()
        val dy = (y - other.y).toFloat()
        return sqrt(dx * dx + dy * dy).toInt()
    }

    operator fun plus(other: Coordinate): Coordinate {
        return Coordinate(x + other.x, y + other.y, z + other.z)
    }

    fun up(): Coordinate {
        return Coordinate(x, y, z + 1)
    }

    override fun luaMetatable(lua: Lua): LuaMetatable {
        return luaMeta
    }

    companion object {
        val Zero = Coordinate(0, 0, 0)

        private fun luaGetHorizontalDistanceTo(lua: Lua): Int {
            val self = lua.checkUserdata<Coordinate>(1)
            val (other, _) = lua.checkCoordinate(2)
            lua.push(self.horizontalDistanceTo(other))
            return 1
        }

        val luaMeta = LuaMappedMetatable(Coordinate::class) {
            readOnly(Coordinate::x)
            readOnly(Coordinate::y)
            readOnly(Coordinate::z)
            readOnly(Coordinate::x, "x")
            readOnly(Coordinate::y, "y")
            readOnly(Coordinate::z, "z")
            callable(::luaGetHorizontalDistanceTo)
        }
    }
}
