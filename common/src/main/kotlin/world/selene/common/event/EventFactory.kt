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
import world.selene.common.event.impl.EventFactoryImpl
import java.util.function.Function

/**
 * Helper for creating [Event] classes.
 */
object EventFactory {
    /**
     * Create an "array-backed" Event instance.
     * 
     * 
     * If your factory simply delegates to the listeners without adding custom behavior,
     * consider using [the other overload][.createArrayBacked]
     * if performance of this event is critical.
     * 
     * @param type           The listener class type.
     * @param invokerFactory The invoker factory, combining multiple listeners into one instance.
     * @param <T>            The listener type.
     * @return The Event instance.
    </T> */
    fun <T : Any> createArrayBacked(type: Class<in T>, invokerFactory: Function<Array<T>, T>): Event<T> {
        return EventFactoryImpl.createArrayBacked(type, invokerFactory)
    }

    /**
     * Create an "array-backed" Event instance with a custom empty invoker,
     * for an event whose `invokerFactory` only delegates to the listeners.
     * 
     *  * If there is no listener, the custom empty invoker will be used.
     *  * **If there is only one listener, that one will be used as the invoker
     * and the factory will not be called.**
     *  * Only when there are at least two listeners will the factory be used.
     * 
     * 
     * 
     * Having a custom empty invoker (of type (...) -&gt; {}) increases performance
     * relative to iterating over an empty array; however, it only really matters
     * if the event is executed thousands of times a second.
     * 
     * @param type           The listener class type.
     * @param emptyInvoker   The custom empty invoker.
     * @param invokerFactory The invoker factory, combining multiple listeners into one instance.
     * @param <T>            The listener type.
     * @return The Event instance.
    </T> */
    fun <T : Any> createArrayBacked(type: Class<T>, emptyInvoker: T, invokerFactory: Function<Array<T>, T>): Event<T> {
        return createArrayBacked(type) { listeners: Array<T> ->
            if (listeners.isEmpty()) {
                return@createArrayBacked emptyInvoker
            } else if (listeners.size == 1) {
                return@createArrayBacked listeners[0]
            } else {
                return@createArrayBacked invokerFactory.apply(listeners)
            }
        }
    }

    /**
     * Create an array-backed event with a list of default phases that get invoked in order.
     * Exposing the identifiers of the default phases as `public static final` constants is encouraged.
     * 
     * 
     * An event phase is a named group of listeners, which may be ordered before or after other groups of listeners.
     * This allows some listeners to take priority over other listeners.
     * Adding separate events should be considered before making use of multiple event phases.
     * 
     * 
     * Phases may be freely added to events created with any of the factory functions,
     * however using this function is preferred for widely used event phases.
     * If more phases are necessary, discussion with the author of the Event is encouraged.
     * 
     * 
     * Refer to [Event.addPhaseOrdering] for an explanation of event phases.
     * 
     * @param type           The listener class type.
     * @param invokerFactory The invoker factory, combining multiple listeners into one instance.
     * @param defaultPhases  The default phases of this event, in the correct order. Must contain [Event.DEFAULT_PHASE].
     * @param <T>            The listener type.
     * @return The Event instance.
    </T> */
    fun <T : Any> createWithPhases(
        type: Class<in T>,
        invokerFactory: Function<Array<T>, T>,
        vararg defaultPhases: Identifier
    ): Event<T> {
        EventFactoryImpl.ensureContainsDefault(defaultPhases)
        EventFactoryImpl.ensureNoDuplicates(defaultPhases)

        val event = createArrayBacked<T>(type, invokerFactory)

        for (i in 1..<defaultPhases.size) {
            event.addPhaseOrdering(defaultPhases[i - 1], defaultPhases[i])
        }

        return event
    }

    inline fun <reified T : Any> arrayBackedEvent(
        crossinline invoker: (Array<T>) -> T
    ) = createArrayBacked(T::class.java) { listeners ->
        invoker(listeners)
    }
}
