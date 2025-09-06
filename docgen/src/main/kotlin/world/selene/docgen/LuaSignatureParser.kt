package world.selene.docgen

data class LuaSignature(
    val returnTypes: List<LuaType>,
    val functionName: String,
    val parameters: List<LuaParameter>
)

data class LuaParameter(
    val type: LuaType,
    val name: String
)

sealed class LuaType {
    data class Simple(val name: String) : LuaType() {
        val type = "simple"
    }
    data class Union(val either: List<LuaType>) : LuaType() {
        val type = "union"
    }
    data class Table(val elementType: LuaType? = null, val keyType: LuaType? = null) : LuaType() {
        val type = "table"
    }
    data class StructuredTable(val fields: Map<String, LuaType>) : LuaType() {
        val type = "structuredTable"
    }
    data class Function(val parameters: List<LuaParameter>, val returnTypes: List<LuaType>) : LuaType() {
        val type = "function"
    }
    data class Literal(val value: String) : LuaType() {
        val type = "literal"
    }

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
        is Literal -> value
    }
}

class LuaSignatureParser {

    private enum class TokenType {
        IDENTIFIER,
        STRING_LITERAL, // "string" or 'string'
        NUMBER_LITERAL, // 123 or 123.45
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

    fun parseProperty(propertyString: String): LuaParameter {
        tokens = tokenize(propertyString)
        currentIndex = 0

        if (tokens.isEmpty() || tokens.all { it.type == TokenType.WHITESPACE }) {
            throw IllegalArgumentException("Invalid property string: $propertyString")
        }

        skipWhitespace()
        if (isAtEnd()) throw IllegalArgumentException("Invalid property string: $propertyString")

        // Parse property name (can be identifier, string literal, or number literal)
        val nameToken = currentToken()
        if (nameToken?.type !in listOf(TokenType.IDENTIFIER, TokenType.STRING_LITERAL, TokenType.NUMBER_LITERAL)) {
            throw IllegalArgumentException("Invalid property string: $propertyString")
        }
        val propertyName = nameToken!!.value
        val isLiteral = nameToken.type in listOf(TokenType.STRING_LITERAL, TokenType.NUMBER_LITERAL)
        advance()
        skipWhitespace()

        val propertyType = if (isLiteral) {
            // For literals, there's no colon or type notation - the literal IS the type
            LuaType.Literal(propertyName)
        } else {
            // For regular identifiers, expect colon and type
            if (currentToken()?.type != TokenType.COLON) throw IllegalArgumentException("Invalid property string: $propertyString")
            advance()
            skipWhitespace()
            parseType()
        }
        return LuaParameter(propertyType, propertyName)
    }

    fun parseSignature(signatureLine: String): LuaSignature? {
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

                '"', '\'' -> {
                    // Parse string literal
                    val quote = char
                    val start = i
                    i++ // Skip opening quote
                    while (i < input.length && input[i] != quote) {
                        if (input[i] == '\\' && i + 1 < input.length) {
                            i += 2 // Skip escaped character
                        } else {
                            i++
                        }
                    }
                    if (i < input.length) i++ // Skip closing quote
                    tokens.add(Token(TokenType.STRING_LITERAL, input.substring(start, i), start))
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
                    if (char.isDigit() || (char == '-' && i + 1 < input.length && input[i + 1].isDigit())) {
                        // Parse number literal
                        val start = i
                        if (char == '-') i++ // Skip negative sign
                        while (i < input.length && (input[i].isDigit() || input[i] == '.')) {
                            i++
                        }
                        tokens.add(Token(TokenType.NUMBER_LITERAL, input.substring(start, i), start))
                    } else if (char.isLetterOrDigit() || char == '_' || char == '.') {
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

        // Parse parameter name (can be identifier, string literal, or number literal)
        val firstToken = currentToken()
        if (firstToken?.type !in listOf(TokenType.IDENTIFIER, TokenType.STRING_LITERAL, TokenType.NUMBER_LITERAL)) {
            throw IllegalArgumentException("Expected parameter name, got: ${firstToken?.value}")
        }

        val name = firstToken!!.value
        val isLiteral = firstToken.type in listOf(TokenType.STRING_LITERAL, TokenType.NUMBER_LITERAL)
        advance() // Skip name
        skipWhitespace()

        val type = if (isLiteral) {
            // For literals, there's no colon or type notation - the literal IS the type
            LuaType.Literal(name)
        } else {
            // For regular identifiers, expect colon and type
            if (currentToken()?.type != TokenType.COLON) {
                throw IllegalArgumentException("Expected ':' after parameter name '$name'")
            }
            advance() // Skip colon
            skipWhitespace()
            parseType()
        }

        return LuaParameter(type, name)
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
