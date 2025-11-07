package ai.codeeditor.core

import kotlin.math.max
import kotlin.math.min

/**
 * Cursor position in (line, column) coordinates.
 */
data class Cursor(
    val line: Int,
    val column: Int
)

/**
 * Text selection range in absolute offsets.
 */
data class Selection(
    val start: Int,
    val end: Int
) {
    init {
        require(start >= 0) { "start must be >= 0" }
        require(end >= 0) { "end must be >= 0" }
    }

    fun normalized(): Selection =
        if (start <= end) this else Selection(end, start)

    fun isEmpty(): Boolean = start == end
}

/**
 * Immutable editor state snapshot.
 */
data class EditorState(
    val cursor: Cursor = Cursor(0, 0),
    val selection: Selection? = null,
    val scrollY: Float = 0f,
    val viewportLines: Int = 60
)

/**
 * EditorController manages mutable state and editing operations.
 * Separates model (TextBuffer) from view state (cursor, selection, scroll).
 */
class EditorController(
    val buffer: TextBuffer,
    initialState: EditorState = EditorState(),
    private val onStateChange: () -> Unit = {}
) {
    private var desiredColumn: Int? = null
    private val undoManager = UndoManager()

    var state: EditorState = clampState(initialState)
        private set(value) {
            field = value
            onStateChange()
        }

    /**
     * Restore cursor and selection state (used by undo/redo).
     */
    internal fun restoreState(cursor: Cursor, selection: Selection?) {
        state = state.copy(cursor = cursor, selection = selection)
        desiredColumn = null
    }

    private fun selectionNormalized(): Selection? = state.selection?.normalized()

    fun caretOffset(): Int = buffer.toOffset(state.cursor.line, state.cursor.column)

    fun setScrollY(scrollY: Float) {
        state = state.copy(scrollY = scrollY.coerceAtLeast(0f))
    }

    fun adjustScrollBy(delta: Float) {
        setScrollY(state.scrollY + delta)
    }

    fun setViewportLines(lines: Int) {
        state = state.copy(viewportLines = max(1, lines))
    }

    fun clearSelection() {
        state = state.copy(selection = null)
    }

    fun setCursorPosition(line: Int, column: Int, select: Boolean) {
        val clampedLine = line.coerceIn(0, max(0, buffer.lineCount - 1))
        val lineStart = buffer.lineStart(clampedLine)
        val lineEnd = buffer.lineEnd(clampedLine)
        val lineLength = lineEnd - lineStart
        val clampedColumn = column.coerceIn(0, lineLength)
        val targetOffset = buffer.toOffset(clampedLine, clampedColumn)
        desiredColumn = null
        setCursorOffset(targetOffset, select)
    }

    fun getSelectedText(): String {
        val selection = selectionNormalized() ?: return ""
        if (selection.isEmpty()) return ""
        return buffer.get(selection.start until selection.end).toString()
    }

    fun selectAll() {
        if (buffer.length == 0) return
        state = state.copy(
            cursor = buffer.toLineCol(buffer.length).let { Cursor(it.first, it.second) },
            selection = Selection(0, buffer.length)
        )
    }

    fun insertText(text: CharSequence) {
        // Allow empty text to delete selection
        val cursorBefore = state.cursor
        val selectionBefore = state.selection
        
        // If text is empty and no selection, do nothing
        if (text.isEmpty() && selectionBefore == null) return
        
        val caretBefore = caretOffset()
        val selection = selectionNormalized()
        val start = selection?.start ?: caretBefore
        val end = selection?.end ?: caretBefore
        val startClamped = start.coerceIn(0, buffer.length)
        val endClamped = end.coerceIn(startClamped, buffer.length)
        
        // Get deleted text for undo
        val deletedText = if (endClamped > startClamped) {
            buffer.get(startClamped until endClamped).toString()
        } else {
            ""
        }
        
        // Calculate cursor position after operation (must be done BEFORE modifying buffer)
        // We need to temporarily calculate what the position will be
        val tempBuffer = SimpleTextBuffer(buffer.get(0 until buffer.length).toString())
        if (deletedText.isNotEmpty()) {
            tempBuffer.delete(startClamped until endClamped)
        }
        tempBuffer.insert(startClamped, text)
        val cursorAfter = tempBuffer.toLineCol(startClamped + text.length).let { Cursor(it.first, it.second) }
        
        // Create command
        val command = if (deletedText.isNotEmpty()) {
            // This is a replace operation
            ReplaceCommand(
                offset = startClamped,
                deletedText = deletedText,
                insertedText = text.toString(),
                cursorBefore = cursorBefore,
                selectionBefore = selectionBefore,
                cursorAfter = cursorAfter,
                selectionAfter = null
            )
        } else {
            // This is a pure insert
            InsertCommand(
                offset = startClamped,
                text = text.toString(),
                cursorBefore = cursorBefore,
                selectionBefore = selectionBefore,
                cursorAfter = cursorAfter,
                selectionAfter = null
            )
        }
        
        // Execute through undo manager
        undoManager.executeCommand(command, buffer, this)
        desiredColumn = null
    }

    fun insertNewLine() = insertText("\n")

    fun deleteBackward() {
        val cursorBefore = state.cursor
        val selectionBefore = state.selection
        val caretBefore = caretOffset()
        val selection = selectionNormalized()
        
        if (selection != null && !selection.isEmpty()) {
            // Delete selection
            val deletedText = buffer.get(selection.start until selection.end).toString()
            // Calculate cursor position after deletion
            val tempBuffer = SimpleTextBuffer(buffer.get(0 until buffer.length).toString())
            tempBuffer.delete(selection.start until selection.end)
            val cursorAfter = tempBuffer.toLineCol(selection.start).let { Cursor(it.first, it.second) }
            
            val command = DeleteCommand(
                offset = selection.start,
                deletedText = deletedText,
                cursorBefore = cursorBefore,
                selectionBefore = selectionBefore,
                cursorAfter = cursorAfter,
                selectionAfter = null
            )
            undoManager.executeCommand(command, buffer, this)
            desiredColumn = null
            return
        }
        
        if (caretBefore == 0) return
        
        val start = caretBefore - 1
        val deletedText = buffer.get(start until caretBefore).toString()
        // Calculate cursor position after deletion
        val tempBuffer = SimpleTextBuffer(buffer.get(0 until buffer.length).toString())
        tempBuffer.delete(start until caretBefore)
        val cursorAfter = tempBuffer.toLineCol(start).let { Cursor(it.first, it.second) }
        
        val command = DeleteCommand(
            offset = start,
            deletedText = deletedText,
            cursorBefore = cursorBefore,
            selectionBefore = selectionBefore,
            cursorAfter = cursorAfter,
            selectionAfter = null
        )
        undoManager.executeCommand(command, buffer, this)
        desiredColumn = null
    }

    fun deleteForward() {
        val cursorBefore = state.cursor
        val selectionBefore = state.selection
        val caretBefore = caretOffset()
        val selection = selectionNormalized()
        
        if (selection != null && !selection.isEmpty()) {
            // Delete selection
            val deletedText = buffer.get(selection.start until selection.end).toString()
            // Calculate cursor position after deletion
            val tempBuffer = SimpleTextBuffer(buffer.get(0 until buffer.length).toString())
            tempBuffer.delete(selection.start until selection.end)
            val cursorAfter = tempBuffer.toLineCol(selection.start).let { Cursor(it.first, it.second) }
            
            val command = DeleteCommand(
                offset = selection.start,
                deletedText = deletedText,
                cursorBefore = cursorBefore,
                selectionBefore = selectionBefore,
                cursorAfter = cursorAfter,
                selectionAfter = null
            )
            undoManager.executeCommand(command, buffer, this)
            desiredColumn = null
            return
        }
        
        if (caretBefore >= buffer.length) return
        
        val deletedText = buffer.get(caretBefore until (caretBefore + 1)).toString()
        val command = DeleteCommand(
            offset = caretBefore,
            deletedText = deletedText,
            cursorBefore = cursorBefore,
            selectionBefore = selectionBefore,
            cursorAfter = cursorBefore, // Cursor stays at same position for deleteForward
            selectionAfter = null
        )
        undoManager.executeCommand(command, buffer, this)
        desiredColumn = null
    }

    /**
     * Undo the last editing operation.
     */
    fun undo() {
        undoManager.undo(buffer, this)
    }
    
    /**
     * Redo the last undone operation.
     */
    fun redo() {
        undoManager.redo(buffer, this)
    }
    
    /**
     * Check if undo is available.
     */
    fun canUndo(): Boolean = undoManager.canUndo()
    
    /**
     * Check if redo is available.
     */
    fun canRedo(): Boolean = undoManager.canRedo()

    fun moveLeft(select: Boolean) {
        val caretBefore = caretOffset()
        val selection = selectionNormalized()
        if (selection != null && !select) {
            desiredColumn = null
            setCursorOffset(selection.start, select = false)
            return
        }
        val target = if (caretBefore > 0) caretBefore - 1 else caretBefore
        desiredColumn = null
        setCursorOffset(target, select)
    }

    fun moveRight(select: Boolean) {
        val caretBefore = caretOffset()
        val selection = selectionNormalized()
        if (selection != null && !select) {
            desiredColumn = null
            setCursorOffset(selection.end, select = false)
            return
        }
        val target = min(buffer.length, caretBefore + 1)
        desiredColumn = null
        setCursorOffset(target, select)
    }

    fun moveUp(select: Boolean) = moveVerticalByLines(-1, select)

    fun moveDown(select: Boolean) = moveVerticalByLines(1, select)

    private fun moveVerticalByLines(deltaLines: Int, select: Boolean) {
        if (deltaLines == 0) return
        val caretBefore = caretOffset()
        val selection = selectionNormalized()
        if (selection != null && !select) {
            val collapse = if (deltaLines < 0) selection.start else selection.end
            desiredColumn = null
            setCursorOffset(collapse, select = false)
            return
        }
        val baseColumn = desiredColumn ?: state.cursor.column
        val maxLine = max(0, buffer.lineCount - 1)
        val targetLine = (state.cursor.line + deltaLines).coerceIn(0, maxLine)
        val newOffset = buffer.toOffset(targetLine, baseColumn)
        desiredColumn = baseColumn
        setCursorOffset(newOffset, select)
    }

    fun moveLineStart(select: Boolean) {
        val newOffset = buffer.lineStart(state.cursor.line)
        desiredColumn = null
        setCursorOffset(newOffset, select)
    }

    fun moveLineEnd(select: Boolean) {
        val newOffset = buffer.toOffset(state.cursor.line, Int.MAX_VALUE)
        desiredColumn = null
        setCursorOffset(newOffset, select)
    }

    fun movePage(deltaLines: Int, lineHeightPx: Float, select: Boolean) {
        if (deltaLines == 0) return
        moveVerticalByLines(deltaLines, select)
        val targetScroll = (state.scrollY + deltaLines * lineHeightPx).coerceAtLeast(0f)
        state = state.copy(scrollY = targetScroll)
    }

    fun ensureCaretVisible(lineHeightPx: Float, viewportHeight: Float) {
        val caretLine = state.cursor.line.coerceAtLeast(0)
        val caretTop = caretLine * lineHeightPx
        val caretBottom = caretTop + lineHeightPx
        val contentHeight = buffer.lineCount * lineHeightPx
        val maxScroll = max(0f, contentHeight - viewportHeight)

        val currentScroll = state.scrollY
        val newScroll = when {
            caretTop < currentScroll -> caretTop
            caretBottom > currentScroll + viewportHeight -> caretBottom - viewportHeight
            else -> currentScroll
        }.coerceIn(0f, maxScroll)

        if (newScroll != currentScroll) {
            state = state.copy(scrollY = newScroll)
        }
    }

    private fun setCursorOffset(targetOffset: Int, select: Boolean) {
        val clamped = targetOffset.coerceIn(0, buffer.length)
        val (line, column) = buffer.toLineCol(clamped)
        val anchor = if (select) {
            val sel = selectionNormalized()
            val caretBefore = caretOffset()
            if (sel != null) {
                if (caretBefore == sel.end) sel.start else sel.end
            } else {
                caretBefore
            }.coerceIn(0, buffer.length)
        } else {
            null
        }
        
        state = if (anchor != null) {
            state.copy(
                cursor = Cursor(line, column),
                selection = Selection(anchor, clamped)
            )
        } else {
            state.copy(
                cursor = Cursor(line, column),
                selection = null
            )
        }
    }

    private fun deleteRange(start: Int, endExclusive: Int) {
        if (start >= endExclusive) return
        buffer.delete(start..(endExclusive - 1))
    }

    private fun clampState(initial: EditorState): EditorState {
        val clampedLine = initial.cursor.line.coerceIn(0, max(0, buffer.lineCount - 1))
        val lineLength = buffer.lineEnd(clampedLine) - buffer.lineStart(clampedLine)
        val clampedColumn = initial.cursor.column.coerceIn(0, max(0, lineLength))
        val clampedSelection = initial.selection?.normalized()?.let {
            Selection(
                start = it.start.coerceIn(0, buffer.length),
                end = it.end.coerceIn(0, buffer.length)
            )
        }
        return initial.copy(
            cursor = Cursor(clampedLine, clampedColumn),
            selection = clampedSelection,
            viewportLines = max(1, initial.viewportLines),
            scrollY = initial.scrollY.coerceAtLeast(0f)
        )
    }
}
