package com.seleneworlds.common.statemachine

interface StateMachineListener<T> {
    fun onStateChanged(previousState: State<T>?, newState: State<T>)
}
