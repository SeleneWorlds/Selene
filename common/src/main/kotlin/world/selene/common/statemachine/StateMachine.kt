package world.selene.common.statemachine

class StateMachine<TStateProvider, TStateData> {
    private val states = mutableMapOf<String, State<TStateData>>()
    private val transitions = mutableMapOf<String, MutableList<StateTransition<TStateProvider>>>()
    var currentState: State<TStateData>? = null; private set
    private var defaultState: String? = null
    private val listeners = mutableSetOf<StateMachineListener<TStateData>>()
    private var initialized = false

    fun addState(state: State<TStateData>): StateMachine<TStateProvider, TStateData> {
        states[state.name] = state
        transitions[state.name] = mutableListOf()
        return this
    }

    fun addTransition(fromState: String, transition: StateTransition<TStateProvider>): StateMachine<TStateProvider, TStateData> {
        transitions[fromState]?.add(transition)
            ?: throw IllegalArgumentException("State '$fromState' does not exist")
        return this
    }

    fun setDefaultState(stateName: String): StateMachine<TStateProvider, TStateData> {
        if (!states.containsKey(stateName)) {
            throw IllegalArgumentException("State '$stateName' does not exist")
        }
        defaultState = stateName
        return this
    }

    private fun ensureInitialized() {
        if (initialized) return
        defaultState?.let { stateName ->
            val state = states[stateName]
            if (state != null) {
                currentState = state
                state.enter()
            }
        }
        initialized = true
    }

    fun update(stateProvider: TStateProvider) {
        ensureInitialized()

        val current = currentState ?: return

        transitions[current.name]?.forEach { transition ->
            if (transition.shouldTransition(stateProvider)) {
                transitionTo(transition.targetState)
                transition.trigger()
                return
            }
        }
    }

    fun transitionTo(stateName: String) {
        val targetState = states[stateName]
            ?: throw IllegalArgumentException("State '$stateName' does not exist")

        val previousState = currentState
        currentState?.exit()
        currentState = targetState
        targetState.enter()
        listeners.forEach { it.onStateChanged(previousState, targetState) }
    }

    fun addListener(listener: StateMachineListener<TStateData>) {
        listeners.add(listener)

        if (initialized) {
            currentState?.let { listener.onStateChanged(null, it) }
        }
    }

    fun removeListener(listener: StateMachineListener<TStateData>) {
        listeners.remove(listener)
    }

}
