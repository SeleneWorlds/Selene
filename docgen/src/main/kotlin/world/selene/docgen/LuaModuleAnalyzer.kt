package world.selene.docgen

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
    val global: String?,
    val description: String?,
    val functions: Map<String, Function>,
    val fields: Map<String, Field>
)

data class Function(
    val name: String,
    val description: String? = null,
    val signatures: List<LuaSignature> = emptyList()
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
    private val signatureParser = LuaSignatureParser()

    fun analyzeProject(baseDir: File): Map<String, LuaModuleInfo> {
        val moduleInfos = mutableMapOf<String, LuaModuleInfo>()
        val srcPath = Paths.get(baseDir.absolutePath, "src", "main", "kotlin")
        if (Files.exists(srcPath)) {
            Files.walk(srcPath)
                .filter { Files.isRegularFile(it) && it.toString().endsWith(".kt") }
                .forEach { kotlinFile ->
                    moduleInfos.putAll(analyzeKotlinFile(baseDir, kotlinFile.toFile()))
                }
        }
        return moduleInfos
    }

    private fun analyzeKotlinFile(baseDir: File, file: File): Map<String, LuaModuleInfo> {
        val content = file.readText()
        val virtualFile = LightVirtualFile(file.name, KotlinFileType.INSTANCE, content)
        val psiFile = PsiManager.getInstance(environment.project)
            .findFile(virtualFile) as? KtFile ?: return emptyMap()

        val luaModuleClasses = psiFile.collectDescendantsOfType<KtClassOrObject>().filter { ktClass: KtClassOrObject ->
            ktClass.getSuperTypeList()?.entries?.any { superType ->
                superType.text == "LuaModule"
            } == true
        }
        return luaModuleClasses.map { extractLuaModuleInfo(baseDir, it, file) }.associateBy { it.moduleName }
    }

    private fun extractLuaModuleInfo(baseDir: File, clazz: KtClassOrObject, file: File): LuaModuleInfo {
        val className = clazz.name ?: throw IllegalArgumentException("Class name is null")
        val relativePath = file.relativeTo(baseDir).path

        val methods = clazz.collectDescendantsOfType<KtNamedFunction>()
            .filter { it.name != null }
            .associateBy { it.name!! }
        val properties = clazz.collectDescendantsOfType<KtProperty>()
            .filter { it.name != null && it.isMember }
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
    ): Map<String, Function> {
        val result = mutableMapOf<String, Function>()
        for (args in registrations) {
            if (args.size != 2) continue
            val name = args[0].getArgumentExpression()?.text?.removeSurrounding("\"")
            val value = args[1].getArgumentExpression()?.text?.substringAfterLast("::")
            if (name != null && value != null) {
                val method = methods[value]
                    ?: throw IllegalArgumentException("Method '$value' not found in class ${clazz.name}")
                val description = parseDocs(method.docComment)
                val signatures = signatureParser.extractSignatures(description)
                val cleanedDescription = removeSignaturesBlock(description)
                result[name] = Function(name, cleanedDescription, signatures)
            }
        }
        return result
    }

    private fun extractFields(
        clazz: KtClassOrObject,
        registrations: List<List<KtValueArgument>>,
        properties: Map<String, KtProperty>
    ): Map<String, Field> {
        val result = mutableMapOf<String, Field>()
        for (args in registrations) {
            val name = args[0].getArgumentExpression()?.text?.removeSurrounding("\"")
            val value = args[1].getArgumentExpression()?.text
            if (name != null && value != null) {
                val property = properties[value]
                    ?: throw IllegalArgumentException("Property '$value' not found in class ${clazz.name}")
                val type = property.typeReference?.text
                    ?: throw IllegalArgumentException("Property '$value' is missing explicit type annotation")
                val description = parseDocs(property.docComment)
                result[name] = Field(type, name, description)
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

    private fun removeSignaturesBlock(description: String?): String? {
        if (description == null) return null
        
        val signatureBlockPattern = Regex("```signatures\\s*\\n(.*?)\\n```", RegexOption.DOT_MATCHES_ALL)
        val cleanedDescription = description.replace(signatureBlockPattern, "").trim()
        
        return cleanedDescription.ifEmpty { null }
    }

    fun close() {
        Disposer.dispose(disposable)
    }
}
