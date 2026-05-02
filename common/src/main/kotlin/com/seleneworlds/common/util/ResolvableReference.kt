package com.seleneworlds.common.util

import java.lang.ref.WeakReference
import kotlin.reflect.KClass

class ResolvableReference<TID : Any, TObject : Any>(
    val type: KClass<TObject>,
    val id: TID,
    private val resolver: ReferenceResolver<TID, TObject>,
    initialObject: TObject? = null
) {

    private var cachedObject: WeakReference<TObject>? = null

    init {
        if (initialObject != null) {
            cachedObject = WeakReference(initialObject)
        }
    }

    fun resolve(): TObject? {
        if (cachedObject != null) {
            val resolved = cachedObject?.get()
            if (resolved != null) {
                return resolved
            } else {
                cachedObject = null
            }
        }
        val resolved = resolver.dereferencePersisted(id)
        cachedObject = WeakReference(resolved)
        return resolved
    }

    override fun toString(): String {
        return "Ref(${type.simpleName}, $id)"
    }

}
