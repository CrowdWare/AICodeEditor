package ai.codeeditor.ui.paint

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.Paragraph
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.createFontFamilyResolver
import androidx.compose.ui.platform.LocalFontFamilyResolver

/**
 * TextPainter renders text lines using Skia Paragraph for precise control.
 * No BasicTextField - direct Canvas drawing.
 * Supports both plain text and styled (syntax highlighted) text.
 */
class TextPainter(
    private val density: Density,
    private val fontFamilyResolver: FontFamily.Resolver,
    private val textStyle: TextStyle = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontSize = 14.sp,
        color = Color.White
    )
) {
    private val cache = mutableMapOf<ParagraphCacheKey, Paragraph>()

    data class ParagraphCacheKey(
        val text: String,
        val width: Float,
        val style: TextStyle,
        val annotationHash: Int = 0  // for styled text caching
    )

    /**
     * Build or retrieve cached paragraph for a line of plain text.
     */
    fun getParagraph(text: String, maxWidth: Float): Paragraph {
        val key = ParagraphCacheKey(text, maxWidth, textStyle)
        return cache.getOrPut(key) {
            Paragraph(
                text = text,
                style = textStyle,
                width = maxWidth,
                density = density,
                fontFamilyResolver = fontFamilyResolver,
                maxLines = 1
            )
        }
    }

    /**
     * Build or retrieve cached paragraph for styled text (syntax highlighted).
     */
    fun getStyledParagraph(annotatedString: AnnotatedString, maxWidth: Float): Paragraph {
        val key = ParagraphCacheKey(
            annotatedString.text,
            maxWidth,
            textStyle,
            annotatedString.spanStyles.hashCode()
        )
        return cache.getOrPut(key) {
            Paragraph(
                text = annotatedString.text,
                style = textStyle,
                spanStyles = annotatedString.spanStyles,
                width = maxWidth,
                density = density,
                fontFamilyResolver = fontFamilyResolver,
                maxLines = 1
            )
        }
    }

    /**
     * Draw a single line of plain text at the specified position.
     */
    fun DrawScope.drawLine(
        text: String,
        x: Float,
        y: Float,
        maxWidth: Float
    ) {
        val paragraph = getParagraph(text, maxWidth)
        drawContext.canvas.save()
        drawContext.canvas.translate(x, y)
        paragraph.paint(drawContext.canvas)
        drawContext.canvas.restore()
    }

    /**
     * Draw a single line of styled text (syntax highlighted) at the specified position.
     */
    fun DrawScope.drawStyledLine(
        annotatedString: AnnotatedString,
        x: Float,
        y: Float,
        maxWidth: Float
    ) {
        val paragraph = getStyledParagraph(annotatedString, maxWidth)
        drawContext.canvas.save()
        drawContext.canvas.translate(x, y)
        paragraph.paint(drawContext.canvas)
        drawContext.canvas.restore()
    }

    /**
     * Get horizontal position for a column in a line (for caret placement).
     * Works with both plain and styled text.
     */
    fun getHorizontalPosition(text: String, column: Int, maxWidth: Float): Float {
        val paragraph = getParagraph(text, maxWidth)
        val offset = column.coerceIn(0, text.length)
        return paragraph.getHorizontalPosition(offset, true)
    }

    /**
     * Clear cache (call when font/style changes).
     */
    fun clearCache() {
        cache.clear()
    }
}
