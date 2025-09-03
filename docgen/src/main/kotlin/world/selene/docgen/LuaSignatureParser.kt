package world.selene.docgen

data class LuaSignature(
    val returnTypes: List<LuaType>,
    val functionName: String,
    val parameters: List<LuaParameter>
)

data class LuaParameter(
    val type: LuaType,
    val name: String,
    val isOptional: Boolean = false
)

sealed class LuaType {
    data class Simple(val name: String) : LuaType()
    data class Union(val either: List<LuaType>) : LuaType()
    data class Table(val elementType: LuaType? = null, val keyType: LuaType? = null) : LuaType()
    data class StructuredTable(val fields: Map<String, LuaType>) : LuaType()
    data class Function(val parameters: List<LuaParameter>, val returnTypes: List<LuaType>) : LuaType()

    override fun toString(): String = when (this) {
        is Simple -> name
        is Union -> either.joinToString("|")
        is Table -> when {
            elementType != null && keyType != null -> "table[$keyType, $elementType]"
            elementType != null -> "table[$elementType]"
            else -> "table"
        }
        is StructuredTable -> {
            val fieldsStr = fields.entries.joinToString(", ") { "${it.key}: ${it.value}" }
            "table{$fieldsStr}"
        }
        is Function -> {
            val paramStr = parameters.joinToString(", ") { "${it.name}: ${it.type}" }
            val returnStr = if (returnTypes.isNotEmpty()) returnTypes.joinToString(", ") else "void"
            "function($paramStr) -> $returnStr"
        }
    }
}

class LuaSignatureParser {
    
    private enum class TokenType {
        IDENTIFIER,
        PIPE,           // |
        COMMA,          // ,
        LPAREN,         // (
        RPAREN,         // )
        LBRACE,         // {
        RBRACE,         // }
        LBRACKET,       // [
        RBRACKET,       // ]
        COLON,          // :
        ARROW,          // ->
        WHITESPACE,
        EOF
    }
    
    private data class Token(
        val type: TokenType,
        val value: String,
        val position: Int
    )
    
    private var tokens: List<Token> = emptyList()
    private var currentIndex = 0
    
    fun extractSignatures(documentation: String?): List<LuaSignature> {
        if (documentation == null) return emptyList()
        
        val signatures = mutableListOf<LuaSignature>()
        val signatureBlockPattern = Regex("```signatures\\s*\\n(.*?)\\n```", RegexOption.DOT_MATCHES_ALL)
        signatureBlockPattern.findAll(documentation).forEach { match ->
            val luaCode = match.groupValues[1].trim()
            signatures.addAll(parseSignatures(luaCode))
        }
        
        return signatures
    }
    
    private fun parseSignatures(luaCode: String): List<LuaSignature> {
        val signatures = mutableListOf<LuaSignature>()
        val lines = luaCode.lines().map { it.trim() }.filter { it.isNotEmpty() }
        
        for (line in lines) {
            try {
                parseSignature(line)?.let { signatures.add(it) }
            } catch (e: Exception) {
                // Skip malformed signatures but continue parsing others
                println("Warning: Failed to parse signature: $line - ${e.message}")
            }
        }
        
        return signatures
    }
    
    private fun parseSignature(signatureLine: String): LuaSignature? {
        tokens = tokenize(signatureLine)
        currentIndex = 0
        
        if (tokens.isEmpty() || tokens.all { it.type == TokenType.WHITESPACE }) {
            return null
        }
        
        skipWhitespace()

        // First token should be the function name identifier
        if (currentToken()?.type != TokenType.IDENTIFIER) {
            return null
        }

        val functionName: String = currentToken()!!.value
        advance()
        skipWhitespace()
        
        // Expect opening parenthesis
        if (currentToken()?.type != TokenType.LPAREN) {
            return null
        }
        
        advance() // Skip '('
        val parameters = parseParameterList()
        skipWhitespace()
        
        // Parse return types if arrow is present
        val returnTypes = mutableListOf<LuaType>()
        if (currentToken()?.type == TokenType.ARROW) {
            advance() // Skip '->'
            skipWhitespace()
            returnTypes.addAll(parseTypeList())
        }
        
        return LuaSignature(returnTypes, functionName, parameters)
    }
    
    private fun tokenize(input: String): List<Token> {
        val tokens = mutableListOf<Token>()
        var i = 0
        
        while (i < input.length) {
            when (val char = input[i]) {
                ' ', '\t', '\n', '\r' -> {
                    val start = i
                    while (i < input.length && input[i].isWhitespace()) i++
                    tokens.add(Token(TokenType.WHITESPACE, input.substring(start, i), start))
                }
                '|' -> {
                    tokens.add(Token(TokenType.PIPE, "|", i))
                    i++
                }
                ',' -> {
                    tokens.add(Token(TokenType.COMMA, ",", i))
                    i++
                }
                '(' -> {
                    tokens.add(Token(TokenType.LPAREN, "(", i))
                    i++
                }
                ')' -> {
                    tokens.add(Token(TokenType.RPAREN, ")", i))
                    i++
                }
                '{' -> {
                    tokens.add(Token(TokenType.LBRACE, "{", i))
                    i++
                }
                '}' -> {
                    tokens.add(Token(TokenType.RBRACE, "}", i))
                    i++
                }
                ':' -> {
                    tokens.add(Token(TokenType.COLON, ":", i))
                    i++
                }
                '[' -> {
                    tokens.add(Token(TokenType.LBRACKET, "[", i))
                    i++
                }
                ']' -> {
                    tokens.add(Token(TokenType.RBRACKET, "]", i))
                    i++
                }
                '-' -> {
                    if (i + 1 < input.length && input[i + 1] == '>') {
                        tokens.add(Token(TokenType.ARROW, "->", i))
                        i += 2
                    } else {
                        i++
                    }
                }
                else -> {
                    if (char.isLetterOrDigit() || char == '_' || char == '.') {
                        val start = i
                        while (i < input.length && (input[i].isLetterOrDigit() || input[i] == '_' || input[i] == '.')) {
                            i++
                        }
                        tokens.add(Token(TokenType.IDENTIFIER, input.substring(start, i), start))
                    } else {
                        // Skip unknown characters
                        i++
                    }
                }
            }
        }
        
        tokens.add(Token(TokenType.EOF, "", input.length))
        return tokens
    }
    
    private fun parseTypeList(): List<LuaType> {
        val types = mutableListOf<LuaType>()
        
        skipWhitespace()
        if (isAtEnd()) return types
        
        types.add(parseType())
        
        while (!isAtEnd()) {
            skipWhitespace()
            val token = currentToken()
            if (token?.type == TokenType.COMMA) {
                advance()
                skipWhitespace()
                if (!isAtEnd()) {
                    types.add(parseType())
                }
            } else {
                break
            }
        }
        
        return types
    }
    
    private fun parseType(): LuaType {
        skipWhitespace()
        val types = mutableListOf<LuaType>()
        
        // Parse the first type
        types.add(parseSingleType())
        
        // Check for union types (pipe separated)
        while (!isAtEnd()) {
            skipWhitespace()
            if (currentToken()?.type == TokenType.PIPE) {
                advance() // Skip '|'
                skipWhitespace()
                types.add(parseSingleType())
            } else {
                break
            }
        }
        
        return if (types.size > 1) LuaType.Union(types) else types[0]
    }
    
    private fun parseSingleType(): LuaType {
        skipWhitespace()
        val token = currentToken() ?: throw IllegalStateException("Unexpected end of input")
        
        return when {
            token.value == "table" -> parseTableType()
            token.value == "function" -> parseFunctionType()
            token.type == TokenType.IDENTIFIER -> {
                val typeName = token.value
                advance()
                LuaType.Simple(typeName)
            }
            else -> throw IllegalArgumentException("Unexpected token: ${token.value}")
        }
    }
    
    private fun parseTableType(): LuaType {
        advance() // Skip 'table'
        skipWhitespace()
        
        when (currentToken()?.type) {
            TokenType.LBRACKET -> {
                // table[value] or table[key, value]
                advance() // Skip '['
                skipWhitespace()
                
                val firstType = parseSingleType()
                skipWhitespace()
                
                if (currentToken()?.type == TokenType.COMMA) {
                    advance() // Skip ','
                    skipWhitespace()
                    val secondType = parseSingleType()
                    skipWhitespace()
                    
                    if (currentToken()?.type == TokenType.RBRACKET) {
                        advance() // Skip ']'
                    }
                    
                    return LuaType.Table(elementType = secondType, keyType = firstType)
                } else {
                    if (currentToken()?.type == TokenType.RBRACKET) {
                        advance() // Skip ']'
                    }
                    return LuaType.Table(elementType = firstType)
                }
            }
            TokenType.LBRACE -> {
                // table{field: fieldType, otherField: otherFieldType}
                return parseTableWithFields()
            }
            else -> return LuaType.Table()
        }
    }
    
    private fun parseTableWithFields(): LuaType {
        advance() // Skip '{'
        skipWhitespace()
        
        val fields = mutableMapOf<String, LuaType>()
        
        while (!isAtEnd() && currentToken()?.type != TokenType.RBRACE) {
            skipWhitespace()
            
            // Parse field name
            if (currentToken()?.type == TokenType.IDENTIFIER) {
                val fieldName = currentToken()!!.value
                advance()
                skipWhitespace()
                
                // Expect colon
                if (currentToken()?.type == TokenType.COLON) {
                    advance() // Skip ':'
                    skipWhitespace()
                    fields[fieldName] = parseType()
                }
            }
            
            skipWhitespace()
            
            // Skip comma if present
            if (currentToken()?.type == TokenType.COMMA) {
                advance()
            }
        }
        
        // Skip closing brace
        if (currentToken()?.type == TokenType.RBRACE) {
            advance()
        }
        
        return LuaType.StructuredTable(fields)
    }
    
    private fun parseFieldType(): String {
        val typeTokens = mutableListOf<String>()
        var parenDepth = 0
        var bracketDepth = 0
        var braceDepth = 0
        
        while (!isAtEnd()) {
            val token = currentToken()
            when (token?.type) {
                TokenType.LPAREN -> {
                    parenDepth++
                    typeTokens.add("(")
                    advance()
                }
                TokenType.RPAREN -> {
                    parenDepth--
                    typeTokens.add(")")
                    advance()
                    if (parenDepth < 0) break
                }
                TokenType.LBRACKET -> {
                    bracketDepth++
                    typeTokens.add("[")
                    advance()
                }
                TokenType.RBRACKET -> {
                    bracketDepth--
                    typeTokens.add("]")
                    advance()
                    if (bracketDepth < 0) break
                }
                TokenType.LBRACE -> {
                    braceDepth++
                    typeTokens.add("{")
                    advance()
                }
                TokenType.RBRACE -> {
                    if (braceDepth == 0) break // End of current table
                    braceDepth--
                    typeTokens.add("}")
                    advance()
                }
                TokenType.COMMA -> {
                    if (parenDepth == 0 && bracketDepth == 0 && braceDepth == 0) {
                        break // End of current field
                    }
                    typeTokens.add(",")
                    advance()
                }
                TokenType.IDENTIFIER -> {
                    typeTokens.add(token.value)
                    advance()
                }
                TokenType.PIPE -> {
                    typeTokens.add("|")
                    advance()
                }
                TokenType.COLON -> {
                    typeTokens.add(":")
                    advance()
                }
                TokenType.ARROW -> {
                    typeTokens.add("->")
                    advance()
                }
                TokenType.WHITESPACE -> {
                    typeTokens.add(" ")
                    advance()
                }
                else -> {
                    advance() // Skip unknown tokens
                }
            }
        }
        
        return typeTokens.joinToString("").trim()
    }
    
    private fun parseFunctionType(): LuaType {
        advance() // Skip 'function'
        skipWhitespace()
        
        val parameters = mutableListOf<LuaParameter>()
        
        if (currentToken()?.type == TokenType.LPAREN) {
            advance() // Skip '('
            parameters.addAll(parseParameterList())
        }
        
        return LuaType.Function(parameters, emptyList())
    }
    
    /**
     * Parses a parameter list inside parentheses or braces.
     */
    private fun parseParameterList(): List<LuaParameter> {
        val parameters = mutableListOf<LuaParameter>()
        skipWhitespace()
        
        // Handle empty parameter list
        if (currentToken()?.type == TokenType.RPAREN) {
            advance()
            return parameters
        }
        
        while (!isAtEnd()) {
            skipWhitespace()
            
            // Check for end of parameter list
            if (currentToken()?.type == TokenType.RPAREN) {
                advance()
                break
            }
            
            // Parse parameter
            val parameter = parseParameter()
            parameters.add(parameter)
            
            skipWhitespace()
            
            // Check for comma separator
            if (currentToken()?.type == TokenType.COMMA) {
                advance()
            } else if (currentToken()?.type != TokenType.RPAREN) {
                // If no comma and not at end, there might be an error
                break
            }
        }
        
        return parameters
    }
    
    /**
     * Parses a single parameter.
     */
    private fun parseParameter(): LuaParameter {
        skipWhitespace()
        
        // Check if this is an options object (starts with "{")
        if (currentToken()?.type == TokenType.LBRACE) {
            return parseOptionsParameter()
        }
        
        // Parse colon notation: name: type
        val firstToken = currentToken()
        if (firstToken?.type != TokenType.IDENTIFIER) {
            throw IllegalArgumentException("Expected parameter name, got: ${firstToken?.value}")
        }
        
        val name = firstToken.value
        advance() // Skip name
        skipWhitespace()
        
        if (currentToken()?.type != TokenType.COLON) {
            throw IllegalArgumentException("Expected ':' after parameter name '$name'")
        }
        
        advance() // Skip colon
        skipWhitespace()
        val type = parseType()
        
        return LuaParameter(type, name)
    }
    
    /**
     * Parses options parameters like {number volume, number pitch}.
     */
    private fun parseOptionsParameter(): LuaParameter {
        advance() // Skip '{'
        skipWhitespace()
        
        val optionTypes = mutableListOf<String>()
        
        while (!isAtEnd() && currentToken()?.type != TokenType.RBRACE) {
            skipWhitespace()
            
            // Parse colon notation: name: type
            val firstToken = currentToken()
            if (firstToken?.type == TokenType.IDENTIFIER) {
                val paramName = firstToken.value
                advance() // Skip name
                skipWhitespace()
                
                if (currentToken()?.type != TokenType.COLON) {
                    throw IllegalArgumentException("Expected ':' after field name '$paramName' in options parameter")
                }
                
                advance() // Skip colon
                skipWhitespace()
                
                // Parse type using the same robust method as parseFieldType
                val typeName = parseFieldType()
                optionTypes.add("$paramName: $typeName")
            }
            
            skipWhitespace()
            
            // Skip comma if present
            if (currentToken()?.type == TokenType.COMMA) {
                advance()
            }
        }
        
        // Skip closing brace
        if (currentToken()?.type == TokenType.RBRACE) {
            advance()
        }
        
        val optionsTypeName = if (optionTypes.isNotEmpty()) {
            "{${optionTypes.joinToString(", ")}}"
        } else {
            "table"
        }
        
        return LuaParameter(LuaType.Simple(optionsTypeName), "options", isOptional = true)
    }
    
    private fun currentToken(): Token? = if (currentIndex < tokens.size) tokens[currentIndex] else null
    
    private fun advance(): Token? {
        if (!isAtEnd()) currentIndex++
        return if (currentIndex - 1 < tokens.size) tokens[currentIndex - 1] else null
    }
    
    private fun isAtEnd(): Boolean = currentIndex >= tokens.size || currentToken()?.type == TokenType.EOF
    
    private fun skipWhitespace() {
        while (currentToken()?.type == TokenType.WHITESPACE) {
            advance()
        }
    }
}
