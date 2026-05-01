package world.selene.common.lua

interface ReferenceResolver<TID : Any, TObject : Any> {
    fun dereferencePersisted(id: TID): TObject?
}