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
package com.seleneworlds.common.event.impl

import com.seleneworlds.common.data.Identifier
import com.seleneworlds.common.event.Event
import java.util.*
import java.util.function.Function

internal class ArrayBackedEvent<T : Any>(type: Class<in T>, private val invokerFactory: Function<Array<T>, T>) :
    Event<T>() {
    private val lock = Any()

    @Suppress("UNCHECKED_CAST")
    private var handlers: Array<T> = java.lang.reflect.Array.newInstance(type, 0) as Array<T>

    /**
     * Registered event phases.
     */
    private val phases: MutableMap<Identifier, EventPhaseData<T>> = LinkedHashMap<Identifier, EventPhaseData<T>>()

    /**
     * Phases sorted in the correct dependency order.
     */
    private val sortedPhases: MutableList<EventPhaseData<T>> = ArrayList<EventPhaseData<T>>()

    init {
        update()
    }

    fun update() {
        this.invoker = invokerFactory.apply(handlers)
    }

    override fun register(listener: T) {
        register(DEFAULT_PHASE, listener)
    }

    override fun register(phase: Identifier, listener: T) {
        Objects.requireNonNull(phase, "Tried to register a listener for a null phase!")
        Objects.requireNonNull<T>(listener, "Tried to register a null listener!")

        synchronized(lock) {
            getOrCreatePhase(phase, true).addListener(listener)
            rebuildInvoker(handlers.size + 1)
        }
    }

    private fun getOrCreatePhase(id: Identifier, sortIfCreate: Boolean): EventPhaseData<T> {
        var phase = phases[id]

        if (phase == null) {
            phase = EventPhaseData(id, handlers.javaClass.componentType)
            phases[id] = phase
            sortedPhases.add(phase)

            if (sortIfCreate) {
                NodeSorting.sort(sortedPhases, "event phases", Comparator.comparing { it.id })
            }
        }

        return phase
    }

    private fun rebuildInvoker(newLength: Int) {
        // Rebuild handlers.
        if (sortedPhases.size == 1) {
            // Special case with a single phase: use the array of the phase directly.
            handlers = sortedPhases.first().listeners
        } else {
            @Suppress("UNCHECKED_CAST") val newHandlers =
                java.lang.reflect.Array.newInstance(handlers.javaClass.componentType, newLength) as Array<T>
            var newHandlersIndex = 0

            for (existingPhase in sortedPhases) {
                val length: Int = existingPhase.listeners.size
                System.arraycopy(existingPhase.listeners, 0, newHandlers, newHandlersIndex, length)
                newHandlersIndex += length
            }

            handlers = newHandlers
        }

        // Rebuild invoker.
        update()
    }

    override fun addPhaseOrdering(firstPhase: Identifier, secondPhase: Identifier) {
        Objects.requireNonNull(firstPhase, "Tried to add an ordering for a null phase.")
        Objects.requireNonNull(secondPhase, "Tried to add an ordering for a null phase.")
        require(firstPhase != secondPhase) { "Tried to add a phase that depends on itself." }

        synchronized(lock) {
            val first = getOrCreatePhase(firstPhase, false)
            val second = getOrCreatePhase(secondPhase, false)
            SortableNode.link(first, second)
            NodeSorting.sort<EventPhaseData<T>>(this.sortedPhases, "event phases", Comparator.comparing<EventPhaseData<T>, Identifier> { it.id })
            rebuildInvoker(handlers.size)
        }
    }

}
