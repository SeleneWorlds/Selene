package com.seleneworlds.common.observable

interface Observable<T> {
    fun subscribe(observer: Observer<T>)
    fun unsubscribe(observer: Observer<T>)
    fun notifyObservers(data: T)
}