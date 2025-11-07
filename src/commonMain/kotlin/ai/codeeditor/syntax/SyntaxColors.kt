package ai.codeeditor.syntax

import androidx.compose.ui.graphics.Color

/**
 * Color scheme for syntax highlighting (VSCode Dark+ inspired).
 */
object SyntaxColors {
    
    // Token colors
    val keyword = Color(0xFF569CD6)        // Blue - fun, val, class, if
    val type = Color(0xFF4EC9B0)           // Cyan - Int, String, Boolean
    val function = Color(0xFFDCDCAA)       // Yellow - function names
    val string = Color(0xFFCE9178)         // Orange - "text"
    val number = Color(0xFFB5CEA8)         // Light green - 123, 0xFF
    val comment = Color(0xFF6A9955)        // Green - // comments
    val annotation = Color(0xFFDCDCAA)     // Yellow - @Override
    val operator = Color(0xFFD4D4D4)       // Light gray - +, -, =
    val punctuation = Color(0xFFD4D4D4)    // Light gray - (), {}, []
    val identifier = Color(0xFF9CDCFE)     // Light blue - variables
    val default = Color(0xFFD4D4D4)        // Light gray - default
    
    /**
     * Get color for a token type.
     */
    fun getColor(tokenType: TokenType): Color {
        return when (tokenType) {
            TokenType.KEYWORD -> keyword
            TokenType.TYPE -> type
            TokenType.FUNCTION -> function
            TokenType.STRING -> string
            TokenType.NUMBER -> number
            TokenType.COMMENT -> comment
            TokenType.ANNOTATION -> annotation
            TokenType.OPERATOR -> operator
            TokenType.PUNCTUATION -> punctuation
            TokenType.IDENTIFIER -> identifier
            TokenType.WHITESPACE -> default
            TokenType.DEFAULT -> default
        }
    }
}
