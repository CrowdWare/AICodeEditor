package ai.codeeditor.ui

import ai.codeeditor.core.EditorController
import ai.codeeditor.ui.input.Keymap
import ai.codeeditor.ui.paint.TextPainter
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFontFamilyResolver
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.sp
import kotlin.math.max
import kotlin.math.min

/**
 * Canvas-based EditorView - NO BasicTextField.
 * Renders text with Paragraph, handles own cursor/selection/scrolling.
 */
@Composable
fun EditorView(
    controller: EditorController,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val fontFamilyResolver = LocalFontFamilyResolver.current
    val clipboardManager = LocalClipboardManager.current
    val focusRequester = remember { FocusRequester() }
    var viewportSize by remember { mutableStateOf(IntSize.Zero) }
    
    val textPainter = remember(density, fontFamilyResolver) {
        TextPainter(
            density = density,
            fontFamilyResolver = fontFamilyResolver,
            textStyle = TextStyle(
                fontFamily = FontFamily.Monospace,
                fontSize = 14.sp,
                color = Color(0xFFE0E0E0)
            )
        )
    }

    val lineHeight = with(density) { 20.sp.toPx() }
    val leftMargin = 60f
    val topPadding = 8f

    // Request focus on first composition
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    // Update viewport size
    LaunchedEffect(viewportSize) {
        if (viewportSize.height > 0) {
            val lines = (viewportSize.height / lineHeight).toInt()
            controller.setViewportLines(max(1, lines))
        }
    }

    // Read state and buffer directly - the key() in Main.kt handles recomposition
    val state = controller.state
    val buffer = controller.buffer

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF1E1E1E))
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .onSizeChanged { viewportSize = it }
        ) {
            val scrollY = state.scrollY
            val firstVisibleLine = (scrollY / lineHeight).toInt().coerceAtLeast(0)
            val lastVisibleLine = min(
                buffer.lineCount - 1,
                ((scrollY + size.height) / lineHeight).toInt() + 1
            )

            // Draw selection background
            val selection = state.selection?.normalized()
            if (selection != null && !selection.isEmpty()) {
                val (startLine, startCol) = buffer.toLineCol(selection.start)
                val (endLine, endCol) = buffer.toLineCol(selection.end)
                
                for (line in max(startLine, firstVisibleLine)..min(endLine, lastVisibleLine)) {
                    val lineStart = buffer.lineStart(line)
                    val lineEnd = buffer.lineEnd(line)
                    val lineText = buffer.get(lineStart until lineEnd).toString()
                    
                    val selStart = if (line == startLine) startCol else 0
                    val selEnd = if (line == endLine) endCol else lineText.length
                    
                    if (selStart < selEnd) {
                        val y = line * lineHeight - scrollY + topPadding
                        val xStart = leftMargin + textPainter.getHorizontalPosition(
                            lineText, selStart, size.width - leftMargin
                        )
                        val xEnd = leftMargin + textPainter.getHorizontalPosition(
                            lineText, selEnd, size.width - leftMargin
                        )
                        
                        drawRect(
                            color = Color(0xFF264F78),
                            topLeft = Offset(xStart, y),
                            size = androidx.compose.ui.geometry.Size(xEnd - xStart, lineHeight)
                        )
                    }
                }
            }

            // Draw line numbers and text
            for (line in firstVisibleLine..lastVisibleLine) {
                val y = line * lineHeight - scrollY + topPadding
                
                // Line number
                val lineNum = (line + 1).toString()
                with(textPainter) {
                    drawLine(lineNum, 8f, y, leftMargin - 16f)
                }
                
                // Line text
                val lineStart = buffer.lineStart(line)
                val lineEnd = buffer.lineEnd(line)
                val lineText = buffer.get(lineStart until lineEnd).toString()
                
                with(textPainter) {
                    drawLine(lineText, leftMargin, y, size.width - leftMargin)
                }
            }

            // Draw cursor
            val cursorLine = state.cursor.line
            val cursorCol = state.cursor.column
            if (cursorLine in firstVisibleLine..lastVisibleLine) {
                val lineStart = buffer.lineStart(cursorLine)
                val lineEnd = buffer.lineEnd(cursorLine)
                val lineText = buffer.get(lineStart until lineEnd).toString()
                
                val cursorY = cursorLine * lineHeight - scrollY + topPadding
                val cursorX = leftMargin + textPainter.getHorizontalPosition(
                    lineText, cursorCol, size.width - leftMargin
                )
                
                drawLine(
                    color = Color(0xFF528BFF),
                    start = Offset(cursorX, cursorY),
                    end = Offset(cursorX, cursorY + lineHeight),
                    strokeWidth = 2f
                )
            }
        }
        
        // Invisible overlay for mouse input
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(buffer, textPainter, lineHeight, leftMargin) {
                detectTapGestures { offset ->
                    focusRequester.requestFocus()
                    val currentState = controller.state
                    val clickY = offset.y + currentState.scrollY
                    val line = (clickY / lineHeight).toInt().coerceIn(0, buffer.lineCount - 1)
                    
                    val lineStart = buffer.lineStart(line)
                    val lineEnd = buffer.lineEnd(line)
                    val lineText = buffer.get(lineStart until lineEnd).toString()
                    
                    val clickX = offset.x - leftMargin
                    var bestCol = 0
                    var bestDist = Float.MAX_VALUE
                    
                    for (col in 0..lineText.length) {
                        val xPos = textPainter.getHorizontalPosition(
                            lineText, col, size.width - leftMargin
                        )
                        val dist = kotlin.math.abs(xPos - clickX)
                        if (dist < bestDist) {
                            bestDist = dist
                            bestCol = col
                        }
                    }
                    
                    controller.setCursorPosition(line, bestCol, select = false)
                }
            }
                .pointerInput(buffer, textPainter, lineHeight, leftMargin) {
                detectDragGestures(
                    onDragStart = { offset ->
                        focusRequester.requestFocus()
                        val currentState = controller.state
                        val clickY = offset.y + currentState.scrollY
                        val line = (clickY / lineHeight).toInt().coerceIn(0, buffer.lineCount - 1)
                        
                        val lineStart = buffer.lineStart(line)
                        val lineEnd = buffer.lineEnd(line)
                        val lineText = buffer.get(lineStart until lineEnd).toString()
                        
                        val clickX = offset.x - leftMargin
                        var bestCol = 0
                        var bestDist = Float.MAX_VALUE
                        
                        for (col in 0..lineText.length) {
                            val xPos = textPainter.getHorizontalPosition(
                                lineText, col, size.width - leftMargin
                            )
                            val dist = kotlin.math.abs(xPos - clickX)
                            if (dist < bestDist) {
                                bestDist = dist
                                bestCol = col
                            }
                        }
                        
                        controller.setCursorPosition(line, bestCol, select = false)
                    },
                    onDrag = { change, _ ->
                        val offset = change.position
                        val currentState = controller.state
                        val dragY = offset.y + currentState.scrollY
                        val line = (dragY / lineHeight).toInt().coerceIn(0, buffer.lineCount - 1)
                        
                        val lineStart = buffer.lineStart(line)
                        val lineEnd = buffer.lineEnd(line)
                        val lineText = buffer.get(lineStart until lineEnd).toString()
                        
                        val dragX = offset.x - leftMargin
                        var bestCol = 0
                        var bestDist = Float.MAX_VALUE
                        
                        for (col in 0..lineText.length) {
                            val xPos = textPainter.getHorizontalPosition(
                                lineText, col, size.width - leftMargin
                            )
                            val dist = kotlin.math.abs(xPos - dragX)
                            if (dist < bestDist) {
                                bestDist = dist
                                bestCol = col
                            }
                        }
                        
                        controller.setCursorPosition(line, bestCol, select = true)
                    }
                )
            }
                .focusRequester(focusRequester)
                .focusable()
                .onPreviewKeyEvent { event ->
                    val handled = Keymap.handleKeyEvent(event, controller, clipboardManager)
                    if (handled) {
                        controller.ensureCaretVisible(lineHeight, viewportSize.height.toFloat())
                    }
                    handled
                }
        )
    }
}
