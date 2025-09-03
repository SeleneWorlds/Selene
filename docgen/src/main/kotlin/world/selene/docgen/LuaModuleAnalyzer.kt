package world.selene.docgen

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.com.intellij.openapi.util.Disposer
import org.jetbrains.kotlin.com.intellij.psi.PsiManager
import org.jetbrains.kotlin.com.intellij.testFramework.LightVirtualFile
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.kdoc.psi.api.KDoc
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.collectDescendantsOfType
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths

data class LuaModuleInfo(
    val className: String,
    val filePath: String,
    val moduleName: String,
    val side: String,
    val global: String?,
    val description: String?,
    val functions: List<Function>,
    val fields: List<Field>
)

data class Function(
    val name: String,
    val description: String? = null
)

data class Field(
    val type: String,
    val name: String,
    val description: String? = null
)

class LuaModuleAnalyzer {
    private val disposable = Disposer.newDisposable()
    private val environment = KotlinCoreEnvironment.createForProduction(
        disposable,
        CompilerConfiguration(),
        EnvironmentConfigFiles.JVM_CONFIG_FILES
    )

    fun analyzeProject(): List<LuaModuleInfo> {
        val moduleInfos = mutableListOf<LuaModuleInfo>()
        val projectDirs = listOf("common", "server", "client")

        for (projectDir in projectDirs) {
            val srcPath = Paths.get(projectDir, "src", "main", "kotlin")
            if (Files.exists(srcPath)) {
                Files.walk(srcPath)
                    .filter { Files.isRegularFile(it) && it.toString().endsWith(".kt") }
                    .forEach { kotlinFile ->
                        moduleInfos.addAll(analyzeKotlinFile(kotlinFile.toFile()))
                    }
            }
        }

        return moduleInfos
    }

    private fun analyzeKotlinFile(file: File): List<LuaModuleInfo> {
        val content = file.readText()
        val virtualFile = LightVirtualFile(file.name, KotlinFileType.INSTANCE, content)
        val psiFile = PsiManager.getInstance(environment.project)
            .findFile(virtualFile) as? KtFile ?: return emptyList()

        val luaModuleClasses = psiFile.collectDescendantsOfType<KtClassOrObject>().filter { ktClass: KtClassOrObject ->
            ktClass.getSuperTypeList()?.entries?.any { superType ->
                superType.text == "LuaModule"
            } == true
        }
        return luaModuleClasses.map { extractLuaModuleInfo(it, file) }
    }

    private fun extractLuaModuleInfo(clazz: KtClassOrObject, file: File): LuaModuleInfo {
        val className = clazz.name ?: throw IllegalArgumentException("Class name is null")
        val relativePath = file.relativeTo(File(".")).path

        val side = when {
            relativePath.startsWith("common/") -> "common"
            relativePath.startsWith("server/") -> "server"
            relativePath.startsWith("client/") -> "client"
            else -> "unknown"
        }

        val methods = clazz.collectDescendantsOfType<KtNamedFunction>()
            .filter { it.name != null }
            .associateBy { it.name!! }
        val properties = clazz.collectDescendantsOfType<KtProperty>()
            .filter { it.name != null }
            .associateBy { it.name!! }
        val moduleName = properties["name"]?.initializer?.text?.removeSurrounding("\"")
            ?: throw IllegalArgumentException("Module name is null")
        val registerAsGlobal = properties["registerAsGlobal"]?.initializer?.text?.toBoolean()
        val classDescription = parseDocs(clazz.docComment)

        val registrations = clazz.collectDescendantsOfType<KtNamedFunction>()
            .find { function -> function.name == "register" }
            ?.collectDescendantsOfType<KtCallExpression>()
            ?.filter { it.calleeExpression is KtNameReferenceExpression }
            ?.groupBy({ (it.calleeExpression as KtNameReferenceExpression).getReferencedName() }, { it.valueArguments })
            ?: emptyMap()
        val functions = extractFunctions(clazz, registrations["register"] ?: emptyList(), methods)
        val fields = extractFields(clazz, registrations["set"] ?: emptyList(), properties)
        return LuaModuleInfo(
            className = className,
            filePath = relativePath,
            moduleName = moduleName,
            side = side,
            global = if (registerAsGlobal == true) moduleName else null,
            description = classDescription,
            functions = functions,
            fields = fields
        )
    }

    private fun extractFunctions(
        clazz: KtClassOrObject,
        registrations: List<List<KtValueArgument>>,
        methods: Map<String, KtNamedFunction>
    ): List<Function> {
        val result = mutableListOf<Function>()
        for (args in registrations) {
            if (args.size != 2) continue
            val name = args[0].getArgumentExpression()?.text?.removeSurrounding("\"")
            val value = args[1].getArgumentExpression()?.text?.substringAfterLast("::")
            if (name != null && value != null) {
                val method = methods[value]
                    ?: throw IllegalArgumentException("Method '$value' not found in class ${clazz.name}")
                val description = parseDocs(method.docComment)
                result.add(Function(name, description))
            }
        }
        return result
    }

    private fun extractFields(
        clazz: KtClassOrObject,
        registrations: List<List<KtValueArgument>>,
        properties: Map<String, KtProperty>
    ): List<Field> {
        val result = mutableListOf<Field>()
        for (args in registrations) {
            val name = args[0].getArgumentExpression()?.text?.removeSurrounding("\"")
            val value = args[1].getArgumentExpression()?.text
            if (name != null && value != null) {
                val property = properties[value]
                    ?: throw IllegalArgumentException("Property '$value' not found in class ${clazz.name}")
                val type = property.typeReference?.text
                    ?: throw IllegalArgumentException("Property '$value' is missing explicit type annotation")
                val description = parseDocs(property.docComment)
                result.add(Field(type, name, description))
            }
        }
        return result
    }

    private fun parseDocs(docComment: KDoc?): String? {
        return docComment?.let { comment ->
            val text = comment.text
                .removePrefix("/**")
                .removeSuffix("*/")
                .lines()
                .joinToString("\n") { line ->
                    line.trim().removePrefix("*").trim()
                }
                .trim()

            text.ifEmpty { null }
        }
    }

    fun close() {
        Disposer.dispose(disposable)
    }
}

fun main() {
    val analyzer = LuaModuleAnalyzer()

    try {
        val moduleInfos = analyzer.analyzeProject()

        val mapper = ObjectMapper().registerKotlinModule()
        val jsonOutput = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(moduleInfos)

        val outputFile = File("lua-modules.json")
        outputFile.writeText(jsonOutput)

        println("\nAnalysis complete!")
        println("Found ${moduleInfos.size} LuaModule implementations")
        println("Output written to: ${outputFile.absolutePath}")

    } finally {
        analyzer.close()
    }
}
