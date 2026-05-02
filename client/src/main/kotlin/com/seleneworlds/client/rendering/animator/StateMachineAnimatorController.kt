package com.seleneworlds.client.rendering.animator

import com.seleneworlds.client.entity.Entity
import com.seleneworlds.common.statemachine.State
import com.seleneworlds.common.statemachine.StateMachine
import com.seleneworlds.common.statemachine.StateMachineListener

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
