package com.seleneworlds.client.particles

import kotlinx.serialization.json.Json
import com.seleneworlds.common.data.Identifier
import com.seleneworlds.common.data.json.FileBasedRegistry

class ParticleSystemRegistry(json: Json) : FileBasedRegistry<ParticleSystemDefinition>(
    json, "client", "particles", ParticleSystemDefinition::class, ParticleSystemDefinition.serializer()
) {
    companion object { val IDENTIFIER = Identifier.withDefaultNamespace("particles") }
}
