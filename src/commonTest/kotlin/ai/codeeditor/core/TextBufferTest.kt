package ai.codeeditor.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TextBufferTest {

    @Test
    fun testEmptyBuffer() {
        val buffer = SimpleTextBuffer("")
        assertEquals(0, buffer.length)
        assertEquals(1, buffer.lineCount)
        assertEquals(0, buffer.lineStart(0))
        assertEquals(0, buffer.lineEnd(0))
    }

    @Test
    fun testInitialContent() {
        val buffer = SimpleTextBuffer("Hello\nWorld")
        assertEquals(11, buffer.length)
        assertEquals(2, buffer.lineCount)
        assertEquals("Hello", buffer.get(0..4).toString())
        assertEquals("World", buffer.get(6..10).toString())
    }

    @Test
    fun testInsertAtStart() {
        val buffer = SimpleTextBuffer("World")
        buffer.insert(0, "Hello ")
        assertEquals("Hello World", buffer.snapshot())
        assertEquals(11, buffer.length)
    }

    @Test
    fun testInsertInMiddle() {
        val buffer = SimpleTextBuffer("Hello World")
        buffer.insert(6, "Beautiful ")
        assertEquals("Hello Beautiful World", buffer.snapshot())
    }

    @Test
    fun testInsertAtEnd() {
        val buffer = SimpleTextBuffer("Hello")
        buffer.insert(5, " World")
        assertEquals("Hello World", buffer.snapshot())
    }

    @Test
    fun testInsertNewline() {
        val buffer = SimpleTextBuffer("HelloWorld")
        buffer.insert(5, "\n")
        assertEquals(2, buffer.lineCount)
        assertEquals("Hello\nWorld", buffer.snapshot())
        assertEquals(0, buffer.lineStart(0))
        assertEquals(5, buffer.lineEnd(0))
        assertEquals(6, buffer.lineStart(1))
    }

    @Test
    fun testDeleteRange() {
        val buffer = SimpleTextBuffer("Hello World")
        buffer.delete(5..5) // Delete space
        assertEquals("HelloWorld", buffer.snapshot())
        assertEquals(10, buffer.length)
    }

    @Test
    fun testDeleteMultipleChars() {
        val buffer = SimpleTextBuffer("Hello Beautiful World")
        buffer.delete(5..15) // Delete " Beautiful "
        assertEquals("HelloWorld", buffer.snapshot())
    }

    @Test
    fun testDeleteNewline() {
        val buffer = SimpleTextBuffer("Hello\nWorld")
        buffer.delete(5..5) // Delete newline
        assertEquals(1, buffer.lineCount)
        assertEquals("HelloWorld", buffer.snapshot())
    }

    @Test
    fun testToOffsetAndBack() {
        val buffer = SimpleTextBuffer("Line1\nLine2\nLine3")
        
        // Test line 0
        assertEquals(0, buffer.toOffset(0, 0))
        assertEquals(5, buffer.toOffset(0, 5))
        
        // Test line 1
        assertEquals(6, buffer.toOffset(1, 0))
        assertEquals(11, buffer.toOffset(1, 5))
        
        // Test line 2
        assertEquals(12, buffer.toOffset(2, 0))
        assertEquals(17, buffer.toOffset(2, 5))
    }

    @Test
    fun testToLineCol() {
        val buffer = SimpleTextBuffer("Line1\nLine2\nLine3")
        
        assertEquals(Pair(0, 0), buffer.toLineCol(0))
        assertEquals(Pair(0, 5), buffer.toLineCol(5))
        assertEquals(Pair(1, 0), buffer.toLineCol(6))
        assertEquals(Pair(1, 5), buffer.toLineCol(11))
        assertEquals(Pair(2, 0), buffer.toLineCol(12))
        assertEquals(Pair(2, 5), buffer.toLineCol(17))
    }

    @Test
    fun testRoundTripConversion() {
        val buffer = SimpleTextBuffer("abc\ndef\nghij\nk")
        
        for (offset in 0..buffer.length) {
            val (line, col) = buffer.toLineCol(offset)
            val backToOffset = buffer.toOffset(line, col)
            assertEquals(offset, backToOffset, "Round trip failed for offset $offset")
        }
    }

    @Test
    fun testMultipleEdits() {
        val buffer = SimpleTextBuffer("Hello")
        buffer.insert(5, " World")
        assertEquals("Hello World", buffer.snapshot())
        
        buffer.insert(11, "!")
        assertEquals("Hello World!", buffer.snapshot())
        
        buffer.delete(5..5)
        assertEquals("HelloWorld!", buffer.snapshot())
        
        buffer.insert(5, "\n")
        assertEquals(2, buffer.lineCount)
        assertEquals("Hello\nWorld!", buffer.snapshot())
    }

    @Test
    fun testLineStartEnd() {
        val buffer = SimpleTextBuffer("Line1\nLine22\nL3")
        
        assertEquals(0, buffer.lineStart(0))
        assertEquals(5, buffer.lineEnd(0))
        
        assertEquals(6, buffer.lineStart(1))
        assertEquals(12, buffer.lineEnd(1))
        
        assertEquals(13, buffer.lineStart(2))
        assertEquals(15, buffer.lineEnd(2))
    }

    @Test
    fun testEmptyLines() {
        val buffer = SimpleTextBuffer("A\n\nC")
        assertEquals(3, buffer.lineCount)
        
        assertEquals("A", buffer.get(buffer.lineStart(0) until buffer.lineEnd(0)).toString())
        assertEquals("", buffer.get(buffer.lineStart(1) until buffer.lineEnd(1)).toString())
        assertEquals("C", buffer.get(buffer.lineStart(2) until buffer.lineEnd(2)).toString())
    }

    @Test
    fun testLargeFile() {
        val lines = (1..1000).joinToString("\n") { "Line $it with some content" }
        val buffer = SimpleTextBuffer(lines)
        
        assertEquals(1000, buffer.lineCount)
        assertTrue(buffer.length > 20000)
        
        val (line, col) = buffer.toLineCol(buffer.length / 2)
        assertTrue(line > 400 && line < 600)
    }
}
