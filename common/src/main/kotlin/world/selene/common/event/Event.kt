/*
 * Copyright (c) 2016, 2017, 2018, 2019 FabricMC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package world.selene.common.event

import world.selene.common.data.Identifier
import world.selene.common.event.EventPhases.DEFAULT
import kotlin.concurrent.Volatile

/**
 * Base class for Selene's event implementations based on Fabric's event implementation.
 * 
 * @param <T> The listener type.
 * @see EventFactory
</T> */
abstract class Event<T : Any> {
    /**
     * The invoker field. This should be updated by the implementation to
     * always refer to an instance containing all code that should be
     * executed upon event emission.
     */
    @Volatile
    protected lateinit var invoker: T

    /**
     * Returns the invoker instance.
     * 
     * 
     * An "invoker" is an object which hides multiple registered
     * listeners of type T under one instance of type T, executing
     * them and leaving early as necessary.
     * 
     * @return The invoker instance.
     */
    fun invoker(): T {
        return invoker
    }

    /**
     * Register a listener to the event, in the default phase.
     * Have a look at [.addPhaseOrdering] for an explanation of event phases.
     * 
     * @param listener The desired listener.
     */
    abstract fun register(listener: T)

    /**
     * Register a listener to the event for the specified phase.
     * Have a look at [EventFactory.createWithPhases] for an explanation of event phases.
     * 
     * @param phase Identifier of the phase this listener should be registered for. It will be created if it didn't exist yet.
     * @param listener The desired listener.
     */
    abstract fun register(phase: Identifier, listener: T)

    /**
     * Request that listeners registered for one phase be executed before listeners registered for another phase.
     * Relying on the default phases supplied to [EventFactory.createWithPhases] should be preferred over manually
     * registering phase ordering dependencies.
     * 
     * 
     * Incompatible ordering constraints such as cycles will lead to inconsistent behavior:
     * some constraints will be respected and some will be ignored. If this happens, a warning will be logged.
     * 
     * @param firstPhase The identifier of the phase that should run before the other. It will be created if it didn't exist yet.
     * @param secondPhase The identifier of the phase that should run after the other. It will be created if it didn't exist yet.
     */
    abstract fun addPhaseOrdering(firstPhase: Identifier, secondPhase: Identifier)

    companion object {
        /**
         * The identifier of the default phase.
         * Have a look at [EventFactory.createWithPhases] for an explanation of event phases.
         */
        val DEFAULT_PHASE: Identifier = DEFAULT
    }
}
