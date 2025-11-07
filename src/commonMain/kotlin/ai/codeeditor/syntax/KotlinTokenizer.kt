package ai.codeeditor.syntax

/**
 * Simple regex-based tokenizer for Kotlin syntax highlighting.
 * Processes one line at a time for performance.
 */
class KotlinTokenizer {
    
    private val kotlinKeywords = setOf(
        "abstract", "actual", "annotation", "as", "break", "by", "catch", "class",
        "companion", "const", "constructor", "continue", "crossinline", "data",
        "delegate", "do", "dynamic", "else", "enum", "expect", "external", "false",
        "field", "file", "final", "finally", "for", "fun", "get", "if", "import",
        "in", "infix", "init", "inline", "inner", "interface", "internal", "is",
        "lateinit", "noinline", "null", "object", "open", "operator", "out",
        "override", "package", "param", "private", "property", "protected", "public",
        "receiver", "reified", "return", "sealed", "set", "setparam", "super",
        "suspend", "tailrec", "this", "throw", "true", "try", "typealias", "typeof",
        "val", "var", "vararg", "when", "where", "while"
    )
    
    private val kotlinTypes = setOf(
        "Any", "Array", "Boolean", "Byte", "Char", "Double", "Float", "Int", "Long",
        "Nothing", "Short", "String", "Unit", "UByte", "UInt", "ULong", "UShort",
        "List", "Set", "Map", "MutableList", "MutableSet", "MutableMap",
        "Sequence", "Pair", "Triple"
    )
    
    /**
     * Tokenize a single line of Kotlin code.
     * Supports multi-line comments via state.
     * Fix: Multiline comment state is now local to each tokenize call.
     */
    fun tokenize(lineText: String, lineNumber: Int, multilineState: MultilineState = MultilineState()): LineTokens {
        val tokens = mutableListOf<Token>()
        var pos = 0
        var inMultilineComment = multilineState.inComment

        // Fix: If line is inside multiline comment, mark entire line as COMMENT (even if "*/" not found)
        if (inMultilineComment) {
            val endIndex = lineText.indexOf("*/")
            if (endIndex != -1) {
                tokens.add(Token(
                    TokenType.COMMENT,
                    0,
                    endIndex + 2,
                    lineText.substring(0, endIndex + 2)
                ))
                pos = endIndex + 2
                inMultilineComment = false
                multilineState.inComment = false
            } else {
                tokens.add(Token(
                    TokenType.COMMENT,
                    0,
                    lineText.length,
                    lineText
                ))
                pos = lineText.length
                multilineState.inComment = true
            }
        }

        while (pos < lineText.length) {
            val remaining = lineText.substring(pos)

            when {
                // Multi-line comment start
                remaining.startsWith("/*") -> {
                    val endIndex = remaining.indexOf("*/")
                    if (endIndex != -1) {
                        tokens.add(Token(
                            TokenType.COMMENT,
                            pos,
                            pos + endIndex + 2,
                            remaining.substring(0, endIndex + 2)
                        ))
                        pos += endIndex + 2
                        inMultilineComment = false
                        multilineState.inComment = false
                    } else {
                        tokens.add(Token(
                            TokenType.COMMENT,
                            pos,
                            lineText.length,
                            remaining
                        ))
                        pos = lineText.length
                        inMultilineComment = true
                        multilineState.inComment = true
                    }
                }

                // Whitespace
                remaining.matches(Regex("^\\s+.*")) -> {
                    val match = Regex("^\\s+").find(remaining)!!
                    tokens.add(Token(
                        TokenType.WHITESPACE,
                        pos,
                        pos + match.value.length,
                        match.value
                    ))
                    pos += match.value.length
                }

                // Single-line comment
                remaining.startsWith("//") -> {
                    val commentText = remaining
                    tokens.add(Token(
                        TokenType.COMMENT,
                        pos,
                        lineText.length,
                        commentText
                    ))
                    pos = lineText.length
                }

                // Multi-line comment start
                remaining.startsWith("/*") -> {
                    val endIndex = remaining.indexOf("*/")
                    if (endIndex != -1) {
                        // End of comment in same line
                        tokens.add(Token(
                            TokenType.COMMENT,
                            pos,
                            pos + endIndex + 2,
                            remaining.substring(0, endIndex + 2)
                        ))
                        pos += endIndex + 2
                        inMultilineComment = false
                    } else {
                        // Start of multiline comment, continues to next line
                        tokens.add(Token(
                            TokenType.COMMENT,
                            pos,
                            lineText.length,
                            remaining
                        ))
                        pos = lineText.length
                        inMultilineComment = true
                    }
                }
                
                // String literals
                remaining.startsWith("\"") -> {
                    var i = 1
                    var escaped = false
                    while (i < remaining.length) {
                        if (escaped) {
                            escaped = false
                        } else if (remaining[i] == '\\') {
                            escaped = true
                        } else if (remaining[i] == '"') {
                            i++
                            break
                        }
                        i++
                    }
                    val stringText = remaining.substring(0, i)
                    tokens.add(Token(
                        TokenType.STRING,
                        pos,
                        pos + i,
                        stringText
                    ))
                    pos += i
                }
                
                // Char literals
                remaining.startsWith("'") -> {
                    var i = 1
                    var escaped = false
                    while (i < remaining.length) {
                        if (escaped) {
                            escaped = false
                        } else if (remaining[i] == '\\') {
                            escaped = true
                        } else if (remaining[i] == '\'') {
                            i++
                            break
                        }
                        i++
                    }
                    val charText = remaining.substring(0, i)
                    tokens.add(Token(
                        TokenType.STRING,
                        pos,
                        pos + i,
                        charText
                    ))
                    pos += i
                }
                
                // Annotation
                remaining.startsWith("@") && remaining.length > 1 && remaining[1].isLetter() -> {
                    val match = Regex("^@[a-zA-Z_][a-zA-Z0-9_]*").find(remaining)!!
                    tokens.add(Token(
                        TokenType.ANNOTATION,
                        pos,
                        pos + match.value.length,
                        match.value
                    ))
                    pos += match.value.length
                }
                
                // Numbers (hex, binary, decimal, float)
                remaining.matches(Regex("^(0[xX][0-9a-fA-F_]+|0[bB][01_]+|\\d+\\.\\d+[fF]?|\\d+[LlFf]?).*")) -> {
                    val match = Regex("^(0[xX][0-9a-fA-F_]+|0[bB][01_]+|\\d+\\.\\d+[fF]?|\\d+[LlFf]?)").find(remaining)!!
                    tokens.add(Token(
                        TokenType.NUMBER,
                        pos,
                        pos + match.value.length,
                        match.value
                    ))
                    pos += match.value.length
                }
                
                // Identifiers, keywords, types
                remaining[0].isLetter() || remaining[0] == '_' -> {
                    val match = Regex("^[a-zA-Z_][a-zA-Z0-9_]*").find(remaining)!!
                    val word = match.value
                    val type = when {
                        word in kotlinKeywords -> TokenType.KEYWORD
                        word in kotlinTypes -> TokenType.TYPE
                        // Simple heuristic: uppercase start = Type
                        word[0].isUpperCase() -> TokenType.TYPE
                        // Check if followed by '(' = function call
                        remaining.length > word.length && remaining[word.length] == '(' -> TokenType.FUNCTION
                        else -> TokenType.IDENTIFIER
                    }
                    tokens.add(Token(
                        type,
                        pos,
                        pos + word.length,
                        word
                    ))
                    pos += word.length
                }
                
                // Operators and punctuation
                else -> {
                    val char = remaining[0]
                    val type = when (char) {
                        in "(){}[]" -> TokenType.PUNCTUATION
                        in ";:,." -> TokenType.PUNCTUATION
                        in "+-*/%=<>!&|^~?" -> TokenType.OPERATOR
                        else -> TokenType.DEFAULT
                    }
                    tokens.add(Token(
                        type,
                        pos,
                        pos + 1,
                        char.toString()
                    ))
                    pos++
                }
            }
        }
        
        return LineTokens(
            line = lineNumber,
            tokens = tokens,
            lineHash = lineText.hashCode()
        )
    }
}

/**
 * Tracks multiline comment state between lines.
 */
class MultilineState(var inComment: Boolean = false)
