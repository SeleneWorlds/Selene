package com.seleneworlds.client.timeline

import kotlinx.serialization.json.Json
import com.seleneworlds.common.data.Identifier
import com.seleneworlds.common.data.json.FileBasedRegistry

class TimelineRegistry(json: Json) : FileBasedRegistry<TimelineDefinition>(
    json, "client", "timelines", TimelineDefinition::class, TimelineDefinition.serializer()
) {
    companion object { val IDENTIFIER = Identifier.withDefaultNamespace("timelines") }
}
