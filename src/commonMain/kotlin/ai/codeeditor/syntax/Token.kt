package ai.codeeditor.syntax

/**
 * Token types for syntax highlighting.
 */
enum class TokenType {
    KEYWORD,        // fun, val, var, class, if, else, etc.
    IDENTIFIER,     // variable/function names
    STRING,         // "text"
    NUMBER,         // 123, 0xFF, 3.14
    COMMENT,        // // single line, /* multi line */
    OPERATOR,       // +, -, *, /, =, ==, etc.
    PUNCTUATION,    // (, ), {, }, [, ], ;, :, ,
    WHITESPACE,     // spaces, tabs
    TYPE,           // Int, String, Boolean (types)
    FUNCTION,       // function calls/declarations
    ANNOTATION,     // @Override, @Composable
    DEFAULT         // fallback
}

/**
 * A token represents a syntactic unit in a line of text.
 */
data class Token(
    val type: TokenType,
    val start: Int,     // start offset in line
    val end: Int,       // end offset in line (exclusive)
    val text: String
) {
    val length: Int get() = end - start
}

/**
 * Result of tokenizing a line.
 */
data class LineTokens(
    val line: Int,
    val tokens: List<Token>,
    val lineHash: Int  // for cache validation
)
