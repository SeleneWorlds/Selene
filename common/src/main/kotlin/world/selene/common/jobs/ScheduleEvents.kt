package world.selene.common.jobs

import world.selene.common.event.EventFactory.arrayBackedEvent

class ScheduleEvents {
    fun interface Second {
        fun second()

        companion object {
            val EVENT = arrayBackedEvent<Second> { listeners ->
                Second { listeners.forEach { it.second() } }
            }
        }
    }

    fun interface Minute {
        fun minute()

        companion object {
            val EVENT = arrayBackedEvent<Minute> { listeners ->
                Minute { listeners.forEach { it.minute() } }
            }
        }
    }

    fun interface Hour {
        fun hour()

        companion object {
            val EVENT = arrayBackedEvent<Hour> { listeners ->
                Hour { listeners.forEach { it.hour() } }
            }
        }
    }
}
