package world.selene.docgen

import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.collectDescendantsOfType
import java.io.File

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

class LuaModuleAnalyzer : BaseLuaAnalyzer() {

    fun analyzeProject(baseDir: File): Map<String, LuaModuleInfo> {
        return analyzeProjectStructure(baseDir, ::analyzeKotlinFile)
    }

    private fun analyzeKotlinFile(baseDir: File, file: File): Map<String, LuaModuleInfo> {
        val psiFile = parseKotlinFile(file) ?: return emptyMap()
        val luaModuleClasses = psiFile.collectDescendantsOfType<KtClassOrObject>().filter { ktClass ->
            ktClass.getSuperTypeList()?.entries?.any { superType ->
                superType.text == "LuaModule"
            } == true
        }
        return luaModuleClasses.map { extractLuaModuleInfo(baseDir, it, file) }.associateBy { it.moduleName }
    }

    private fun extractLuaModuleInfo(baseDir: File, clazz: KtClassOrObject, file: File): LuaModuleInfo {
        val className = clazz.name ?: throw IllegalArgumentException("Class name is null")
        val relativePath = file.relativeTo(baseDir).path

        val methods = buildFunctionLookup(clazz)
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
                val documentation = parseDocs(method.docComment)
                val signatures = extractSignatures(documentation)
                val description = removeDocumentationBlocks(documentation, "signatures")
                result[name] = Function(name, description, signatures)
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

}
