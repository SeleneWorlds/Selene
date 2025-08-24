package world.selene.common.lua

interface LuaReferencable<TID : Any, TObject: Any> {
    fun luaReference(): LuaReference<TID, TObject>
}