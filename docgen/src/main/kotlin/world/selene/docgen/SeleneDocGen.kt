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
    val analyzer = LuaModuleAnalyzer()

    try {
        val modulesBySide = mapOf(
            Side.COMMON to analyzer.analyzeProject(File(baseDir, "common")),
            Side.CLIENT to analyzer.analyzeProject(File(baseDir, "client")),
            Side.SERVER to analyzer.analyzeProject(File(baseDir, "server"))
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
    } finally {
        analyzer.close()
    }
}
