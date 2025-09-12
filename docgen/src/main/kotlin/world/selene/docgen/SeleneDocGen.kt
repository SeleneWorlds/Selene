package world.selene.docgen

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.kjetland.jackson.jsonSchema.JsonSchemaGenerator
import world.selene.client.sounds.AudioDefinition
import world.selene.client.rendering.visual.VisualDefinition
import world.selene.common.entities.EntityDefinition
import world.selene.common.entities.component.ComponentDefinition
import world.selene.common.sounds.SoundDefinition
import world.selene.common.tiles.TileDefinition
import world.selene.common.tiles.transitions.TransitionDefinition
import java.io.File
import java.io.FileWriter

enum class Side(val order: Int) {
    COMMON(1),
    SERVER(2),
    CLIENT(3);

    val dirName: String
        get() {
            return "${order}.${name.lowercase()}"
        }
}

fun main(args: Array<String>) {
    val baseDir = args.getOrNull(0) ?: "."
    val moduleAnalyzer = LuaModuleAnalyzer()
    val classAnalyzer = LuaClassAnalyzer()
    val lmlAnalyzer = LmlAnalyzer()
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

        val modulesDir = File("1.modules")
        if (!modulesDir.exists()) {
            modulesDir.mkdirs()
        }

        modulesBySide.forEach { (side, modules) ->
            val sideDir = File(modulesDir, side.dirName)
            if (!sideDir.exists()) {
                sideDir.mkdirs()
            }

            modules.forEach { (moduleName, moduleInfo) ->
                val jsonOutput = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(moduleInfo)
                val outputFile = File(sideDir, "$moduleName.json")
                outputFile.writeText(jsonOutput)
            }
        }

        val classesDir = File("3.classes")
        if (!classesDir.exists()) {
            classesDir.mkdirs()
        }

        classesBySide.forEach { (side, classes) ->
            val sideDir = File(classesDir, side.dirName)
            if (!sideDir.exists()) {
                sideDir.mkdirs()
            }

            classes.forEach { (className, classInfo) ->
                val jsonOutput = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(classInfo)
                val outputFile = File(sideDir, "$className.json")
                outputFile.writeText(jsonOutput)
            }
        }
    } finally {
        moduleAnalyzer.close()
        classAnalyzer.close()
    }

    val dataDir = File("2.data")
    if (!dataDir.exists()) {
        dataDir.mkdirs()
    }
    val commonDataDir = File(dataDir, Side.COMMON.dirName)
    if (!commonDataDir.exists()) {
        commonDataDir.mkdirs()
    }
    val clientDataDir = File(dataDir, Side.CLIENT.dirName)
    if (!clientDataDir.exists()) {
        clientDataDir.mkdirs()
    }

    val schemaGenerator = JsonSchemaGenerator(objectMapper)
    objectMapper.writeValue(
        File(commonDataDir, "TransitionDefinition.json"),
        schemaGenerator.generateJsonSchema(TransitionDefinition::class.java)
    )
    objectMapper.writeValue(
        File(clientDataDir, "AudioDefinition.json"),
        schemaGenerator.generateJsonSchema(AudioDefinition::class.java)
    )
    objectMapper.writeValue(
        File(clientDataDir, "VisualDefinition.json"),
        schemaGenerator.generateJsonSchema(VisualDefinition::class.java)
    )
    objectMapper.writeValue(
        File(commonDataDir, "TileDefinition.json"),
        schemaGenerator.generateJsonSchema(TileDefinition::class.java)
    )
    objectMapper.writeValue(
        File(commonDataDir, "ComponentDefinition.json"),
        schemaGenerator.generateJsonSchema(ComponentDefinition::class.java)
    )
    objectMapper.writeValue(
        File(commonDataDir, "SoundDefinition.json"),
        schemaGenerator.generateJsonSchema(SoundDefinition::class.java)
    )
    objectMapper.writeValue(
        File(commonDataDir, "EntityDefinition.json"),
        schemaGenerator.generateJsonSchema(EntityDefinition::class.java)
    )

    val dtdFile = File("schema.dtd")
    lmlAnalyzer.writeDtd(FileWriter(dtdFile))
}
