package com.seleneworlds.common.observable

interface Observer<T> {
    fun notifyObserver(data: T)
}