package com.seleneworlds.common.jobs

import com.seleneworlds.common.event.EventFactory.arrayBackedEvent
import com.seleneworlds.common.event.EventFactory.catchLog

class ScheduleEvents {
    fun interface Second {
        fun second()

        companion object {
            val EVENT = arrayBackedEvent<Second> { listeners ->
                Second { listeners.forEach { catchLog { it.second() } } }
            }
        }
    }

    fun interface Minute {
        fun minute()

        companion object {
            val EVENT = arrayBackedEvent<Minute> { listeners ->
                Minute { listeners.forEach { catchLog { it.minute() } } }
            }
        }
    }

    fun interface Hour {
        fun hour()

        companion object {
            val EVENT = arrayBackedEvent<Hour> { listeners ->
                Hour { listeners.forEach { catchLog { it.hour() } } }
            }
        }
    }
}
