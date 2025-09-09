package world.selene.client.rendering.animator

import world.selene.client.maps.Entity
import world.selene.common.statemachine.State
import world.selene.common.statemachine.StateMachine
import world.selene.common.statemachine.StateMachineListener

class StateMachineAnimatorController() : AnimatorController, StateMachineListener<ConfiguredAnimation> {
    val stateMachine = StateMachine<Entity, ConfiguredAnimation>().also {
        it.addListener(this)
    }

    override var currentAnimation: ConfiguredAnimation? = null

    fun configure(configure: StateMachine<Entity, ConfiguredAnimation>.() -> Unit): StateMachineAnimatorController {
        stateMachine.configure()
        return this
    }

    override fun update(entity: Entity, delta: Float) {
        stateMachine.update(entity)
    }

    override fun onStateChanged(
        previousState: State<ConfiguredAnimation>?,
        newState: State<ConfiguredAnimation>
    ) {
        currentAnimation = newState.data
    }
}
