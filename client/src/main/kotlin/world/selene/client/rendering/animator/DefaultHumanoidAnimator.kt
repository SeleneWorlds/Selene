package world.selene.client.rendering.animator

import world.selene.client.maps.Entity
import world.selene.common.grid.Direction
import world.selene.common.grid.Grid
import world.selene.common.statemachine.State
import world.selene.common.statemachine.StateMachine
import world.selene.common.statemachine.StateTransition

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
                    targetState = "walk/$direction",
                    condition = { it.direction == direction && it.isInMotion() }
                ))
                stateMachine.addTransition(state.name, StateTransition(
                    targetState = "stationary/$direction",
                    condition = { it.direction == direction && !it.isInMotion() }
                ))
            }
        }

        walkStates.entries.forEach { (_, state) ->
            stateMachine.addState(state)

            for (direction in grid.directions.values) {
                stateMachine.addTransition(state.name, StateTransition(
                    targetState = "walk/$direction",
                    condition = { it.direction == direction && it.isInMotion() }
                ))
                stateMachine.addTransition(state.name, StateTransition(
                    targetState = "stationary/$direction",
                    condition = { it.direction == direction && !it.isInMotion() }
                ))
            }
        }
    }
}