package ai.codeeditor.core

/**
 * Command interface for undo/redo support.
 * Each command represents a reversible editing operation.
 */
interface EditorCommand {
    /**
     * Execute the command (do).
     */
    fun execute(buffer: TextBuffer, controller: EditorController)
    
    /**
     * Reverse the command (undo).
     */
    fun undo(buffer: TextBuffer, controller: EditorController)
    
    /**
     * Check if this command can be merged with another command.
     * Used to group consecutive character insertions.
     */
    fun canMergeWith(other: EditorCommand): Boolean = false
    
    /**
     * Merge this command with another command.
     */
    fun mergeWith(other: EditorCommand): EditorCommand = this
}

/**
 * Command for text insertion.
 */
data class InsertCommand(
    val offset: Int,
    val text: String,
    val cursorBefore: Cursor,
    val selectionBefore: Selection?,
    val cursorAfter: Cursor,
    val selectionAfter: Selection?
) : EditorCommand {
    
    override fun execute(buffer: TextBuffer, controller: EditorController) {
        buffer.insert(offset, text)
        controller.restoreState(cursorAfter, selectionAfter)
    }
    
    override fun undo(buffer: TextBuffer, controller: EditorController) {
        buffer.delete(offset until (offset + text.length))
        controller.restoreState(cursorBefore, selectionBefore)
    }
    
    override fun canMergeWith(other: EditorCommand): Boolean {
        if (other !is InsertCommand) return false
        // Merge consecutive character insertions at adjacent positions
        // Allow merging of already-merged commands (text.length can be > 1)
        return other.text.all { it != '\n' } && // Don't merge if new text contains newline
               text.all { it != '\n' } && // Don't merge if existing text contains newline
               other.offset == offset + text.length &&
               selectionBefore == null &&
               other.selectionBefore == null
    }
    
    override fun mergeWith(other: EditorCommand): EditorCommand {
        if (other !is InsertCommand) return this
        return InsertCommand(
            offset = offset,
            text = text + other.text,
            cursorBefore = cursorBefore,
            selectionBefore = selectionBefore,
            cursorAfter = other.cursorAfter,
            selectionAfter = other.selectionAfter
        )
    }
}

/**
 * Command for text deletion.
 */
data class DeleteCommand(
    val offset: Int,
    val deletedText: String,
    val cursorBefore: Cursor,
    val selectionBefore: Selection?,
    val cursorAfter: Cursor,
    val selectionAfter: Selection?
) : EditorCommand {
    
    override fun execute(buffer: TextBuffer, controller: EditorController) {
        buffer.delete(offset until (offset + deletedText.length))
        controller.restoreState(cursorAfter, selectionAfter)
    }
    
    override fun undo(buffer: TextBuffer, controller: EditorController) {
        buffer.insert(offset, deletedText)
        controller.restoreState(cursorBefore, selectionBefore)
    }
    
    override fun canMergeWith(other: EditorCommand): Boolean {
        if (other !is DeleteCommand) return false
        // Merge consecutive backspace deletions (deleting backwards)
        // Each new backspace deletes one position before the current merged block
        // Example: Delete "f" at 5 -> merge "e" at 4 -> merge "d" at 3
        // After merging "ef" at offset 4, next delete "d" should be at offset 3
        return other.deletedText.all { it != '\n' } && // Don't merge if new deletion contains newline
               deletedText.all { it != '\n' } && // Don't merge if existing deletion contains newline
               other.offset == offset - 1 &&  // New delete is always one position before current offset
               selectionBefore == null &&
               other.selectionBefore == null
    }
    
    override fun mergeWith(other: EditorCommand): EditorCommand {
        if (other !is DeleteCommand) return this
        // Merge: when backspacing, new deleted char comes AFTER old deleted chars
        // Example: Delete 'F' then 'E' from "ABCDEF" -> deleted text should be "EF"
        return DeleteCommand(
            offset = other.offset,
            deletedText = other.deletedText + deletedText,  // Correct order for backspace
            cursorBefore = cursorBefore,  // Keep original before state
            selectionBefore = selectionBefore,
            cursorAfter = other.cursorAfter,
            selectionAfter = other.selectionAfter
        )
    }
}

/**
 * Command for text replacement (delete + insert combined).
 */
data class ReplaceCommand(
    val offset: Int,
    val deletedText: String,
    val insertedText: String,
    val cursorBefore: Cursor,
    val selectionBefore: Selection?,
    val cursorAfter: Cursor,
    val selectionAfter: Selection?
) : EditorCommand {
    
    override fun execute(buffer: TextBuffer, controller: EditorController) {
        if (deletedText.isNotEmpty()) {
            buffer.delete(offset until (offset + deletedText.length))
        }
        if (insertedText.isNotEmpty()) {
            buffer.insert(offset, insertedText)
        }
        controller.restoreState(cursorAfter, selectionAfter)
    }
    
    override fun undo(buffer: TextBuffer, controller: EditorController) {
        if (insertedText.isNotEmpty()) {
            buffer.delete(offset until (offset + insertedText.length))
        }
        if (deletedText.isNotEmpty()) {
            buffer.insert(offset, deletedText)
        }
        controller.restoreState(cursorBefore, selectionBefore)
    }
}

/**
 * Undo/Redo stack manager.
 */
class UndoManager(
    private val maxHistorySize: Int = 1000
) {
    private val undoStack = mutableListOf<EditorCommand>()
    private val redoStack = mutableListOf<EditorCommand>()
    private var isExecuting = false
    
    /**
     * Execute a command and add it to the undo stack.
     */
    fun executeCommand(command: EditorCommand, buffer: TextBuffer, controller: EditorController) {
        if (isExecuting) return // Prevent recursion during undo/redo
        
        isExecuting = true
        try {
            command.execute(buffer, controller)
            
            // Try to merge with the last command
            if (undoStack.isNotEmpty()) {
                val last = undoStack.last()
                if (last.canMergeWith(command)) {
                    undoStack[undoStack.size - 1] = last.mergeWith(command)
                    redoStack.clear()
                    return
                }
            }
            
            // Add as new command
            undoStack.add(command)
            redoStack.clear()
            
            // Limit history size
            if (undoStack.size > maxHistorySize) {
                undoStack.removeAt(0)
            }
        } finally {
            isExecuting = false
        }
    }
    
    /**
     * Undo the last command.
     */
    fun undo(buffer: TextBuffer, controller: EditorController): Boolean {
        if (undoStack.isEmpty() || isExecuting) return false
        
        isExecuting = true
        try {
            val command = undoStack.removeAt(undoStack.size - 1)
            command.undo(buffer, controller)
            redoStack.add(command)
            return true
        } finally {
            isExecuting = false
        }
    }
    
    /**
     * Redo the last undone command.
     */
    fun redo(buffer: TextBuffer, controller: EditorController): Boolean {
        if (redoStack.isEmpty() || isExecuting) return false
        
        isExecuting = true
        try {
            val command = redoStack.removeAt(redoStack.size - 1)
            command.execute(buffer, controller)
            undoStack.add(command)
            return true
        } finally {
            isExecuting = false
        }
    }
    
    /**
     * Check if undo is available.
     */
    fun canUndo(): Boolean = undoStack.isNotEmpty()
    
    /**
     * Check if redo is available.
     */
    fun canRedo(): Boolean = redoStack.isNotEmpty()
    
    /**
     * Clear all history.
     */
    fun clear() {
        undoStack.clear()
        redoStack.clear()
    }
}
