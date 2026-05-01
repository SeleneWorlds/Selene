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
package world.selene.common.event.impl

import com.google.common.collect.MapMaker
import world.selene.common.data.Identifier
import world.selene.common.event.Event
import java.util.*
import java.util.function.Function

object EventFactoryImpl {
    private val ARRAY_BACKED_EVENTS: MutableSet<ArrayBackedEvent<*>> = Collections.newSetFromMap(MapMaker().weakKeys().makeMap<ArrayBackedEvent<*>, Boolean>())

    fun <T : Any> createArrayBacked(type: Class<in T>, invokerFactory: Function<Array<T>, T>): Event<T> {
        val event = ArrayBackedEvent(type, invokerFactory)
        ARRAY_BACKED_EVENTS.add(event)
        return event
    }

    fun ensureContainsDefault(defaultPhases: Array<out Identifier>) {
        for (id in defaultPhases) {
            if (id == Event.DEFAULT_PHASE) {
                return
            }
        }

        throw IllegalArgumentException("The event phases must contain Event.DEFAULT_PHASE.")
    }

    fun ensureNoDuplicates(defaultPhases: Array<out Identifier>) {
        for (i in defaultPhases.indices) {
            for (j in i + 1..<defaultPhases.size) {
                require(defaultPhases[i] != defaultPhases[j]) { "Duplicate event phase: " + defaultPhases[i] }
            }
        }
    }
}
