package world.selene.docgen

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
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

        val mapper = ObjectMapper().registerKotlinModule()
        
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
}
