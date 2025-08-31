package world.selene.common.observable

interface Observer<T> {
    fun notifyObserver(data: T)
}