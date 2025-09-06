package world.selene.docgen

import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.collectDescendantsOfType
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import java.io.File

data class LuaClassInfo(
    val className: String,
    val filePath: String,
    val description: String?,
    val properties: Map<String, PropertyInfo>,
    val functions: Map<String, FunctionInfo>
)

class LuaClassAnalyzer : BaseLuaAnalyzer() {

    fun analyzeProject(baseDir: File): Map<String, LuaClassInfo> {
        return analyzeProjectStructure(baseDir, ::analyzeKotlinFile)
    }

    private fun analyzeKotlinFile(baseDir: File, file: File): Map<String, LuaClassInfo> {
        // Skip the LuaMappedMetatable definition file itself
        if (file.name == "LuaMappedMetatable.kt") {
            return emptyMap()
        }

        val psiFile = parseKotlinFile(file) ?: return emptyMap()
        val luaMetatableInstantiations = psiFile.collectDescendantsOfType<KtCallExpression>().filter { callExpr ->
            val calleeExpr = callExpr.calleeExpression
            calleeExpr is KtNameReferenceExpression && calleeExpr.getReferencedName() == "LuaMappedMetatable"
        }
        return luaMetatableInstantiations.map { extractLuaClassInfo(baseDir, it, file) }
            .associateBy { it.className }
    }

    private fun extractLuaClassInfo(baseDir: File, call: KtCallExpression, file: File): LuaClassInfo {
        val relativePath = file.relativeTo(baseDir).path

        val targetClass = (call.valueArguments.firstOrNull()?.getArgumentExpression() as? KtClassLiteralExpression)
            ?.receiverExpression?.text
            ?: throw IllegalArgumentException("No target class found for LuaMappedMetatable call in ${file.path}")

        val clazz = call.getStrictParentOfType<KtClassOrObject>()
            ?: throw IllegalArgumentException("No class found for LuaMappedMetatable call in ${file.path}")
        val description = parseDocs(clazz.docComment)

        // Extract lambda body to analyze metatable configuration
        val lambdaBody = call.valueArguments.lastOrNull()?.getArgumentExpression() as? KtLambdaExpression
        val registrations = lambdaBody?.collectDescendantsOfType<KtCallExpression>()
            ?.filter { it.calleeExpression is KtNameReferenceExpression }
            ?.groupBy({ (it.calleeExpression as KtNameReferenceExpression).getReferencedName() }, { it.valueArguments })
            ?: emptyMap()
        val methods = buildFunctionLookup(clazz)
        val functions = extractFunctions(clazz, registrations["callable"] ?: emptyList(), methods)
        val properties = extractProperties(
            clazz,
            registrations["getter"] ?: emptyList(),
            registrations["setter"] ?: emptyList(),
            methods
        )
        return LuaClassInfo(
            className = targetClass,
            filePath = relativePath,
            description = description,
            properties = properties,
            functions = functions
        )
    }

    private fun extractFunctions(
        clazz: KtClassOrObject,
        callableCalls: List<List<KtValueArgument?>>,
        methods: Map<String, KtNamedFunction>,
    ): Map<String, FunctionInfo> {
        val functions = mutableMapOf<String, FunctionInfo>()
        for (args in callableCalls) {
            val functionRef = args[0]!!.getArgumentExpression()
            val methodName = (functionRef as? KtCallableReferenceExpression)?.text?.removePrefix("::")
                ?: throw IllegalArgumentException("No callable reference found for function in ${clazz.parent}")
            val documentation = parseDocs(methods[methodName]?.docComment)
            val signatures = extractSignatures(documentation)
            val functionName = methodName.removePrefix("lua")
            val description = removeDocumentationBlocks(documentation, "signatures")
            functions[functionName] = FunctionInfo(functionName, description, signatures)
        }

        return functions
    }

    private fun extractProperties(
        clazz: KtClassOrObject,
        getterCalls: List<List<KtValueArgument?>>,
        setterCalls: List<List<KtValueArgument?>>,
        methods: Map<String, KtNamedFunction>,
    ): Map<String, PropertyInfo> {
        val properties = mutableMapOf<String, PropertyInfo>()
        for (args in getterCalls) {
            val functionRef = args[0]!!.getArgumentExpression()
            val methodName = (functionRef as? KtCallableReferenceExpression)?.text?.removePrefix("::")
                ?: throw IllegalArgumentException("Unknown name for getter in ${clazz.parent}")
            val documentation = parseDocs(methods[methodName]?.docComment)
            val propertyType = extractDocumentationBlocks(documentation, "property").firstOrNull()
                ?.let { signatureParser.parseProperty(it) }
                ?: throw IllegalArgumentException("Unknown property type found for getter $methodName in ${clazz.parent}")
            val propertyName = methodName.removePrefix("luaGet")
            val description = removeDocumentationBlocks(documentation, "property")
            properties[propertyName] =
                PropertyInfo(propertyName, propertyType.type, PropertyAccess.READ_ONLY, description)
        }
        for (args in setterCalls) {
            val functionRef = args[0]!!.getArgumentExpression()
            val methodName = (functionRef as? KtCallableReferenceExpression)?.text?.removePrefix("::")
                ?: throw IllegalArgumentException("Unknown name for setter in ${clazz.parent}")
            val documentation = parseDocs(methods[methodName]?.docComment)
            val propertyType = extractDocumentationBlocks(documentation, "property").firstOrNull()
                ?.let { signatureParser.parseProperty(it) }
                ?: throw IllegalArgumentException("Unknown property type found for setter $methodName in ${clazz.parent}")
            val propertyName = methodName.removePrefix("luaSet")
            val description = removeDocumentationBlocks(documentation, "property")
            properties.compute(propertyName) { _, value ->
                PropertyInfo(
                    propertyName,
                    propertyType.type,
                    if (value != null) PropertyAccess.READ_WRITE else PropertyAccess.WRITE_ONLY,
                    if (value != null) value.description + "\n\n" + description else description
                )
            }
        }

        return properties
    }

}
