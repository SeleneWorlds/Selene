package world.selene.client.controls

import party.iroiro.luajava.Lua
import party.iroiro.luajava.value.LuaValue
import world.selene.common.grid.Direction
import world.selene.common.lua.LuaModule
import world.selene.common.lua.util.checkUserdata
import world.selene.common.lua.util.register

/**
 * Grid-based movement controls.
 */
@Suppress("SameReturnValue")
class LuaMovementGridModule(private val gridMovement: GridMovement) : LuaModule {
    override val name = "selene.movement.grid"

    override fun register(table: LuaValue) {
        table.register("SetMotion", this::luaSetMotion)
        table.register("SetFacing", this::luaSetFacing)
    }

    /**
     * Sets the movement direction for grid-based movement.
     *
     * ```signatures
     * SetMotion(direction: Direction)
     * ```
     */
    private fun luaSetMotion(lua: Lua): Int {
        val direction = lua.checkUserdata(1, Direction::class)
        gridMovement.moveDirection = direction
        return 0
    }

    /**
     * Sets the facing direction for grid-based movement.
     *
     * ```signatures
     * SetFacing(direction: Direction)
     * ```
     */
    private fun luaSetFacing(lua: Lua): Int {
        val direction = lua.checkUserdata(1, Direction::class)
        gridMovement.facingDirection = direction
        return 0
    }
}
