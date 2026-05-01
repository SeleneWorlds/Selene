package world.selene.common.lua

import world.selene.common.util.ResolvableReference

interface IdResolvable<TID : Any, TObject : Any> {
    fun resolvableReference(): ResolvableReference<TID, TObject>
}