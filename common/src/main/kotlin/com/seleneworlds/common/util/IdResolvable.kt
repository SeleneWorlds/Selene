package com.seleneworlds.common.util

interface IdResolvable<TID : Any, TObject : Any> {
    fun resolvableReference(): ResolvableReference<TID, TObject>
}