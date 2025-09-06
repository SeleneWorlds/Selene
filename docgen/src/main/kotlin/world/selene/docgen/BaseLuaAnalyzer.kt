package world.selene.docgen

import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.com.intellij.openapi.util.Disposer
import org.jetbrains.kotlin.com.intellij.psi.PsiManager
import org.jetbrains.kotlin.com.intellij.testFramework.LightVirtualFile
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.kdoc.psi.api.KDoc
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.psiUtil.collectDescendantsOfType
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths

data class PropertyInfo(
    val name: String,
    val type: LuaType,
    val access: PropertyAccess,
    val description: String? = null
)

data class FunctionInfo(
    val name: String,
    val description: String? = null,
    val signatures: List<LuaSignature> = emptyList()
)

enum class PropertyAccess {
    READ_ONLY,
    WRITE_ONLY,
    READ_WRITE
}

abstract class BaseLuaAnalyzer {
    protected val disposable = Disposer.newDisposable()
    protected val environment = KotlinCoreEnvironment.createForProduction(
        disposable,
        CompilerConfiguration(),
        EnvironmentConfigFiles.JVM_CONFIG_FILES
    )
    protected val signatureParser = LuaSignatureParser()

    protected fun <T> analyzeProjectStructure(baseDir: File, analyzer: (File, File) -> Map<String, T>): Map<String, T> {
        val results = mutableMapOf<String, T>()
        val srcPath = Paths.get(baseDir.absolutePath, "src", "main", "kotlin")
        if (Files.exists(srcPath)) {
            Files.walk(srcPath)
                .filter { Files.isRegularFile(it) && it.toString().endsWith(".kt") }
                .forEach { kotlinFile ->
                    results.putAll(analyzer(baseDir, kotlinFile.toFile()))
                }
        }
        return results
    }

    protected fun parseKotlinFile(file: File): KtFile? {
        val content = file.readText()
        val virtualFile = LightVirtualFile(file.name, KotlinFileType.INSTANCE, content)
        return PsiManager.getInstance(environment.project)
            .findFile(virtualFile) as? KtFile
    }

    protected fun buildFunctionLookup(clazz: KtClassOrObject): Map<String, KtNamedFunction> {
        return clazz.collectDescendantsOfType<KtNamedFunction>()
            .filter { it.name != null }
            .associateBy { it.name!! }
    }

    protected fun parseDocs(docComment: KDoc?): String? {
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

    protected fun extractDocumentationBlocks(documentation: String?, blockType: String): List<String> {
        if (documentation == null) return emptyList()

        val blocks = mutableListOf<String>()
        val blockPattern = Regex("```$blockType\\s*\\n(.*?)\\n```", RegexOption.DOT_MATCHES_ALL)
        blockPattern.findAll(documentation).forEach { match ->
            blocks.add(match.groupValues[1].trim())
        }

        return blocks
    }

    protected fun removeDocumentationBlocks(description: String?, blockType: String): String? {
        if (description == null) return null

        val blockPattern = Regex("```$blockType\\s*\\n(.*?)\\n```", RegexOption.DOT_MATCHES_ALL)
        val cleanedDescription = description.replace(blockPattern, "").trim()

        return cleanedDescription.ifEmpty { null }
    }

    protected fun extractSignatures(documentation: String?): List<LuaSignature> {
        val signatureBlocks = extractDocumentationBlocks(documentation, "signatures")
        val signatures = mutableListOf<LuaSignature>()

        signatureBlocks.forEach { luaCode ->
            signatures.addAll(parseSignatures(luaCode))
        }

        return signatures
    }

    private fun parseSignatures(luaCode: String): List<LuaSignature> {
        val signatures = mutableListOf<LuaSignature>()
        val lines = luaCode.lines().map { it.trim() }.filter { it.isNotEmpty() }

        for (line in lines) {
            try {
                signatureParser.parseSignature(line)?.let { signatures.add(it) }
            } catch (e: Exception) {
                println("Warning: Failed to parse signature: $line - ${e.message}")
            }
        }

        return signatures
    }

    fun close() {
        Disposer.dispose(disposable)
    }
}
