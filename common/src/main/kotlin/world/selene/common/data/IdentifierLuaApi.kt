package world.selene.common.data

import party.iroiro.luajava.Lua
import world.selene.common.lua.LuaMappedMetatable
import world.selene.common.lua.util.checkString
import world.selene.common.lua.util.checkUserdata

object IdentifierLuaApi {

    /**
     * Namespace of this custom registry object.
     *
     * ```property
     * Namespace: string
     * ```
     */
    private fun luaGetNamespace(lua: Lua): Int {
        val identifier = lua.checkUserdata<Identifier>(1)
        lua.push(identifier.namespace)
        return 1
    }

    /**
     * Path to this custom registry object.
     *
     * ```property
     * Path: string
     * ```
     */
    private fun luaGetPath(lua: Lua): Int {
        val identifier = lua.checkUserdata<Identifier>(1)
        lua.push(identifier.path)
        return 1
    }

    /**
     * Creates a new identifier with the given prefix added to the path.
     *
     * ```signatures
     * WithPrefix(prefix: string) -> Identifier
     * ```
     */
    private fun luaWithPrefix(lua: Lua): Int {
        val identifier = lua.checkUserdata<Identifier>(1)
        val prefix = lua.checkString(2)
        lua.push(identifier.withPrefix(prefix), Lua.Conversion.FULL)
        return 1
    }

    /**
     * Creates a new identifier with the given suffix added to the path.
     *
     * ```signatures
     * WithSuffix(suffix: string) -> Identifier
     * ```
     */
    private fun luaWithSuffix(lua: Lua): Int {
        val identifier = lua.checkUserdata<Identifier>(1)
        val suffix = lua.checkString(2)
        lua.push(identifier.withSuffix(suffix), Lua.Conversion.FULL)
        return 1
    }

    val luaMeta = LuaMappedMetatable(Identifier::class) {
        getter(::luaGetNamespace)
        getter(::luaGetPath)
        callable(::luaWithPrefix)
        callable(::luaWithSuffix)
    }

}
