package world.selene.common.statemachine

data class StateTransition<TStateProvider>(
    val targetState: String,
    val condition: (TStateProvider) -> Boolean,
    val onTransition: (() -> Unit)? = null
) {
    fun shouldTransition(stateProvider: TStateProvider): Boolean {
        return condition(stateProvider)
    }
    
    fun trigger() {
        onTransition?.invoke()
    }
}
