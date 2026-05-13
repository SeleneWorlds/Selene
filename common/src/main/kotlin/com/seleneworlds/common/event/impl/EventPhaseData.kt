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

/**
 * Data of an [ArrayBackedEvent] phase.
 */
internal class EventPhaseData<T>(val id: Identifier, listenerClass: Class<*>) : SortableNode<EventPhaseData<T>>() {
    @Suppress("UNCHECKED_CAST")
    var listeners: Array<T> = java.lang.reflect.Array.newInstance(listenerClass, 0) as Array<T>

    fun addListener(listener: T) {
        val oldLength = listeners.size
        @Suppress("UNCHECKED_CAST")
        listeners = listeners.copyOf(oldLength + 1) as Array<T>
        listeners[oldLength] = listener
    }

    fun removeListener(listener: T): Boolean {
        val index = listeners.indexOfFirst { it === listener }
        if (index == -1) {
            return false
        }

        @Suppress("UNCHECKED_CAST")
        val newListeners = java.lang.reflect.Array.newInstance(listeners.javaClass.componentType, listeners.size - 1) as Array<T>
        System.arraycopy(listeners, 0, newListeners, 0, index)
        System.arraycopy(listeners, index + 1, newListeners, index, listeners.size - index - 1)
        listeners = newListeners
        return true
    }

    override val description = id.toString()
}
