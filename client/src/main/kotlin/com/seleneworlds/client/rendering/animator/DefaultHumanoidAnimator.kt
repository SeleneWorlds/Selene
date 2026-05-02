package com.seleneworlds.client.rendering.animator

import com.seleneworlds.client.entity.Entity
import com.seleneworlds.common.grid.Direction
import com.seleneworlds.common.grid.Grid
import com.seleneworlds.common.statemachine.State
import com.seleneworlds.common.statemachine.StateMachine
import com.seleneworlds.common.statemachine.StateTransition

class DefaultHumanoidAnimator(private val grid: Grid) {
    fun configure(stateMachine: StateMachine<Entity, ConfiguredAnimation>) {
        val stationaryStates = mutableMapOf<Direction, State<ConfiguredAnimation>>()
        val walkStates = mutableMapOf<Direction, State<ConfiguredAnimation>>()
        for (direction in grid.directions.values) {
            stationaryStates[direction] = State(
                name = "stationary/${direction.name}",
                data = ConfiguredAnimation("stationary/${direction.name}", 1f)
            )
            walkStates[direction] = State(
                name = "walk/${direction.name}",
                data = ConfiguredAnimation("walk/${direction.name}", 1f)
            )
        }

        stationaryStates.entries.forEach { (_, state) ->
            stateMachine.addState(state)

            for (direction in grid.directions.values) {
                stateMachine.addTransition(state.name, StateTransition(
                    targetState = "walk/${direction.name}",
                    condition = { it.direction == direction && it.isInMotion() }
                ))
                stateMachine.addTransition(state.name, StateTransition(
                    targetState = "stationary/${direction.name}",
                    condition = { it.direction == direction && !it.isInMotion() }
                ))
            }
        }

        walkStates.entries.forEach { (_, state) ->
            stateMachine.addState(state)

            for (direction in grid.directions.values) {
                stateMachine.addTransition(state.name, StateTransition(
                    targetState = "walk/${direction.name}",
                    condition = { it.direction == direction && it.isInMotion() }
                ))
                stateMachine.addTransition(state.name, StateTransition(
                    targetState = "stationary/${direction.name}",
                    condition = { it.direction == direction && !it.isInMotion() }
                ))
            }
        }

        stateMachine.setDefaultState("stationary/south")
    }
}