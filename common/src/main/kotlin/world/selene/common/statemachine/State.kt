package world.selene.common.statemachine

data class State<T>(
    val name: String,
    val data: T,
    val onEnter: (() -> Unit)? = null,
    val onExit: (() -> Unit)? = null
) {
    fun enter() {
        onEnter?.invoke()
    }
    
    fun exit() {
        onExit?.invoke()
    }
}
