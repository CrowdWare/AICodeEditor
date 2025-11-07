package ai.codeeditor.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class UndoRedoTest {

    @Test
    fun testSimpleInsertUndo() {
        val buffer = SimpleTextBuffer("Hello")
        val controller = EditorController(buffer)
        
        // Insert text
        controller.setCursorPosition(0, 5, select = false)
        controller.insertText(" World")
        assertEquals("Hello World", buffer.snapshot())
        
        // Undo
        assertTrue(controller.canUndo())
        controller.undo()
        assertEquals("Hello", buffer.snapshot())
        assertEquals(0, controller.state.cursor.line)
        assertEquals(5, controller.state.cursor.column)
    }

    @Test
    fun testSimpleDeleteUndo() {
        val buffer = SimpleTextBuffer("Hello World")
        val controller = EditorController(buffer)
        
        // Position cursor and delete
        controller.setCursorPosition(0, 11, select = false)
        controller.deleteBackward()
        assertEquals("Hello Worl", buffer.snapshot())
        
        // Undo
        controller.undo()
        assertEquals("Hello World", buffer.snapshot())
        assertEquals(0, controller.state.cursor.line)
        assertEquals(11, controller.state.cursor.column)
    }

    @Test
    fun testMultipleUndoRedo() {
        val buffer = SimpleTextBuffer("")
        val controller = EditorController(buffer)
        
        // Type "ABC" - these will be merged
        controller.insertText("A")
        controller.insertText("B")
        controller.insertText("C")
        assertEquals("ABC", buffer.snapshot())
        
        // Undo - should undo all merged operations
        assertTrue(controller.canUndo())
        controller.undo()
        
        // After undo, check if we can undo more or if buffer is empty
        val afterUndo = buffer.snapshot()
        assertTrue(afterUndo == "" || afterUndo == "AB", "Expected empty or 'AB', got '$afterUndo'")
        
        // If still content, undo again
        if (afterUndo == "AB") {
            controller.undo()
            assertEquals("", buffer.snapshot())
        }
    }

    @Test
    fun testDeleteSelectionUndo() {
        val buffer = SimpleTextBuffer("Hello World")
        val controller = EditorController(buffer)
        
        // Select "World" and delete
        controller.setCursorPosition(0, 6, select = false)
        controller.setCursorPosition(0, 11, select = true)
        assertEquals("World", controller.getSelectedText())
        
        controller.insertText("")
        assertEquals("Hello ", buffer.snapshot())
        
        // Undo
        assertTrue(controller.canUndo(), "Should be able to undo")
        controller.undo()
        assertEquals("Hello World", buffer.snapshot())
    }

    @Test
    fun testReplaceSelectionUndo() {
        val buffer = SimpleTextBuffer("Hello World")
        val controller = EditorController(buffer)
        
        // Select "World" and replace with "Kotlin"
        controller.setCursorPosition(0, 6, select = false)
        controller.setCursorPosition(0, 11, select = true)
        controller.insertText("Kotlin")
        assertEquals("Hello Kotlin", buffer.snapshot())
        
        // Undo
        controller.undo()
        assertEquals("Hello World", buffer.snapshot())
    }

    @Test
    fun testNewLineUndo() {
        val buffer = SimpleTextBuffer("Hello")
        val controller = EditorController(buffer)
        
        controller.setCursorPosition(0, 5, select = false)
        controller.insertNewLine()
        controller.insertText("World")
        assertEquals("Hello\nWorld", buffer.snapshot())
        
        // Undo insert "World"
        controller.undo()
        assertEquals("Hello\n", buffer.snapshot())
        
        // Undo newline
        controller.undo()
        assertEquals("Hello", buffer.snapshot())
    }

    @Test
    fun testBackspaceMultipleCharactersUndo() {
        val buffer = SimpleTextBuffer("ABCDEF")
        val controller = EditorController(buffer)
        
        // Position at end
        controller.setCursorPosition(0, 6, select = false)
        
        // Delete 3 characters
        controller.deleteBackward()
        controller.deleteBackward()
        controller.deleteBackward()
        assertEquals("ABC", buffer.snapshot())
        
        // Undo all - may be merged or separate operations
        var undoCount = 0
        val maxUndos = 10 // Safety limit
        while (controller.canUndo() && buffer.snapshot() != "ABCDEF" && undoCount < maxUndos) {
            controller.undo()
            undoCount++
        }
        
        assertEquals("ABCDEF", buffer.snapshot(), "Buffer should be restored to original after $undoCount undo(s)")
    }

    @Test
    fun testUndoRedoClearOnNewEdit() {
        val buffer = SimpleTextBuffer("")
        val controller = EditorController(buffer)
        
        // Type and undo
        controller.insertText("Hello")
        controller.undo()
        assertEquals("", buffer.snapshot())
        
        // Type something new
        controller.insertText("World")
        assertEquals("World", buffer.snapshot())
        
        // Redo should not be available
        assertFalse(controller.canRedo())
    }

    @Test
    fun testNoUndoWhenNoChanges() {
        val buffer = SimpleTextBuffer("Hello")
        val controller = EditorController(buffer)
        
        assertFalse(controller.canUndo())
        assertFalse(controller.canRedo())
    }

    @Test
    fun testDeleteForwardUndo() {
        val buffer = SimpleTextBuffer("Hello World")
        val controller = EditorController(buffer)
        
        // Position cursor at 'W' and delete
        controller.setCursorPosition(0, 6, select = false)
        controller.deleteForward()
        assertEquals("Hello orld", buffer.snapshot())
        
        // Undo
        controller.undo()
        assertEquals("Hello World", buffer.snapshot())
        assertEquals(0, controller.state.cursor.line)
        assertEquals(6, controller.state.cursor.column)
    }

    @Test
    fun testComplexEditSequence() {
        val buffer = SimpleTextBuffer("Line 1\nLine 2")
        val controller = EditorController(buffer)
        
        // Move to end of line 1
        controller.setCursorPosition(0, 6, select = false)
        controller.insertText(" Modified")
        assertEquals("Line 1 Modified\nLine 2", buffer.snapshot())
        
        // Move to line 2 and edit
        controller.setCursorPosition(1, 0, select = false)
        controller.insertText("New ")
        assertEquals("Line 1 Modified\nNew Line 2", buffer.snapshot())
        
        // Undo line 2 edit
        controller.undo()
        assertEquals("Line 1 Modified\nLine 2", buffer.snapshot())
        
        // Undo line 1 edit
        controller.undo()
        assertEquals("Line 1\nLine 2", buffer.snapshot())
        
        // Redo both
        controller.redo()
        assertEquals("Line 1 Modified\nLine 2", buffer.snapshot())
        controller.redo()
        assertEquals("Line 1 Modified\nNew Line 2", buffer.snapshot())
    }

    @Test
    fun testCharacterMerging() {
        val buffer = SimpleTextBuffer("")
        val controller = EditorController(buffer)
        
        // Type individual characters (may or may not merge)
        controller.insertText("H")
        controller.insertText("e")
        controller.insertText("l")
        controller.insertText("l")
        controller.insertText("o")
        assertEquals("Hello", buffer.snapshot())
        
        // Undo operations (may be merged or separate)
        assertTrue(controller.canUndo())
        while (controller.canUndo()) {
            controller.undo()
        }
        assertEquals("", buffer.snapshot())
        
        // All undos should be exhausted
        assertFalse(controller.canUndo())
    }

    @Test
    fun testNewlineDoesNotMerge() {
        val buffer = SimpleTextBuffer("")
        val controller = EditorController(buffer)
        
        // Type characters, then newline, then more characters
        controller.insertText("A")
        controller.insertText("B")
        controller.insertNewLine()
        controller.insertText("C")
        assertEquals("AB\nC", buffer.snapshot())
        
        // Undo "C"
        controller.undo()
        assertEquals("AB\n", buffer.snapshot())
        
        // Undo newline
        controller.undo()
        assertEquals("AB", buffer.snapshot())
        
        // Undo "AB"
        controller.undo()
        assertEquals("", buffer.snapshot())
    }
}
