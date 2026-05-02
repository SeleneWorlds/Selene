package world.selene.common.util

interface IdResolvable<TID : Any, TObject : Any> {
    fun resolvableReference(): ResolvableReference<TID, TObject>
}