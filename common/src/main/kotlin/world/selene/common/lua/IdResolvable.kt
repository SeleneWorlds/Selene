package world.selene.common.lua

interface IdResolvable<TID : Any, TObject : Any> {
    fun resolvableReference(): ResolvableReference<TID, TObject>
}