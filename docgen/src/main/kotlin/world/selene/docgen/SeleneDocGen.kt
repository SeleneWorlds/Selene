package world.selene.docgen

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.kjetland.jackson.jsonSchema.JsonSchemaGenerator
import world.selene.client.data.AudioDefinition
import world.selene.client.data.VisualDefinition
import world.selene.common.data.ComponentDefinition
import world.selene.common.data.EntityDefinition
import world.selene.common.data.SoundDefinition
import world.selene.common.data.TileDefinition
import world.selene.common.data.TransitionDefinition
import java.io.File

enum class Side {
    COMMON,
    SERVER,
    CLIENT
}

fun main(args: Array<String>) {
    val baseDir = args.getOrNull(0) ?: "."
    val moduleAnalyzer = LuaModuleAnalyzer()
    val classAnalyzer = LuaClassAnalyzer()
    val objectMapper = ObjectMapper().registerKotlinModule()

    try {
        val modulesBySide = mapOf(
            Side.COMMON to moduleAnalyzer.analyzeProject(File(baseDir, "common")),
            Side.CLIENT to moduleAnalyzer.analyzeProject(File(baseDir, "client")),
            Side.SERVER to moduleAnalyzer.analyzeProject(File(baseDir, "server"))
        )

        val classesBySide = mapOf(
            Side.COMMON to classAnalyzer.analyzeProject(File(baseDir, "common")),
            Side.CLIENT to classAnalyzer.analyzeProject(File(baseDir, "client")),
            Side.SERVER to classAnalyzer.analyzeProject(File(baseDir, "server"))
        )

        val modulesDir = File("modules")
        if (!modulesDir.exists()) {
            modulesDir.mkdirs()
        }

        val mapper = objectMapper

        modulesBySide.forEach { (side, modules) ->
            val sideDir = File(modulesDir, side.name.lowercase())
            if (!sideDir.exists()) {
                sideDir.mkdirs()
            }

            modules.forEach { (moduleName, moduleInfo) ->
                val jsonOutput = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(moduleInfo)
                val outputFile = File(sideDir, "$moduleName.json")
                outputFile.writeText(jsonOutput)
            }
        }

        val classesDir = File("classes")
        if (!classesDir.exists()) {
            classesDir.mkdirs()
        }

        classesBySide.forEach { (side, classes) ->
            val sideDir = File(classesDir, side.name.lowercase())
            if (!sideDir.exists()) {
                sideDir.mkdirs()
            }

            classes.forEach { (className, classInfo) ->
                val jsonOutput = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(classInfo)
                val outputFile = File(sideDir, "$className.json")
                outputFile.writeText(jsonOutput)
            }
        }
    } finally {
        moduleAnalyzer.close()
        classAnalyzer.close()
    }

    val dataDir = File("data")
    if (!dataDir.exists()) {
        dataDir.mkdirs()
    }

    val schemaGenerator = JsonSchemaGenerator(objectMapper)
    objectMapper.writeValue(
        File(dataDir, "transitions.json"),
        schemaGenerator.generateJsonSchema(TransitionDefinition::class.java)
    )
    objectMapper.writeValue(
        File(dataDir, "audio.json"),
        schemaGenerator.generateJsonSchema(AudioDefinition::class.java)
    )
    objectMapper.writeValue(
        File(dataDir, "visuals.json"),
        schemaGenerator.generateJsonSchema(VisualDefinition::class.java)
    )
    objectMapper.writeValue(
        File(dataDir, "tiles.json"),
        schemaGenerator.generateJsonSchema(TileDefinition::class.java)
    )
    objectMapper.writeValue(
        File(dataDir, "components.json"),
        schemaGenerator.generateJsonSchema(ComponentDefinition::class.java)
    )
    objectMapper.writeValue(
        File(dataDir, "sounds.json"),
        schemaGenerator.generateJsonSchema(SoundDefinition::class.java)
    )
    objectMapper.writeValue(
        File(dataDir, "entities.json"),
        schemaGenerator.generateJsonSchema(EntityDefinition::class.java)
    )
}
