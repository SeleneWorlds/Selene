package world.selene.common.util

interface ReferenceResolver<TID : Any, TObject : Any> {
    fun dereferencePersisted(id: TID): TObject?
}