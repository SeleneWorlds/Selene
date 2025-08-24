package world.selene.common.lua

interface LuaReferenceResolver<TID : Any, TObject : Any> {
    fun luaDereference(id: TID): TObject?
}