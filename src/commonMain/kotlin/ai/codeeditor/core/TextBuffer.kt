package ai.codeeditor.core

import kotlin.math.max

/**
 * TextBuffer interface for efficient text editing operations.
 * No Compose dependencies - pure data structure.
 */
interface TextBuffer {
    val length: Int
    val lineCount: Int

    fun lineStart(line: Int): Int
    fun lineEnd(line: Int): Int

    fun get(range: IntRange): CharSequence

    fun insert(offset: Int, text: CharSequence)
    fun delete(range: IntRange)

    fun toOffset(line: Int, col: Int): Int
    fun toLineCol(offset: Int): Pair<Int, Int>
}

/**
 * Simple StringBuilder-based implementation with LineIndex.
 * Suitable for files up to ~100k lines; can be replaced with Piece Table later.
 */
class SimpleTextBuffer(initialText: CharSequence = "") : TextBuffer {

    private val content = StringBuilder(initialText.toString())
    private val lines = LineIndex.fromText(initialText)

    override val length: Int
        get() = content.length

    override val lineCount: Int
        get() = lines.count

    override fun lineStart(line: Int): Int = lines.lineStart(line)

    override fun lineEnd(line: Int): Int {
        val start = lineStart(line)
        val next = lines.nextLineStart(line)
        if (next <= start) return start
        val newline = content.indexOf('\n', start)
        return if (newline == -1 || newline >= next) next else newline
    }

    override fun get(range: IntRange): CharSequence {
        if (range.isEmpty()) return ""
        val start = range.first.coerceIn(0, length)
        val endExclusive = (range.last + 1).coerceIn(0, length)
        if (start >= endExclusive) return ""
        return content.subSequence(start, endExclusive)
    }

    override fun insert(offset: Int, text: CharSequence) {
        require(offset in 0..length) { "offset $offset out of bounds" }
        if (text.isEmpty()) return
        content.insert(offset, text)
        lines.insert(offset, text)
    }

    override fun delete(range: IntRange) {
        if (range.isEmpty()) return
        val start = range.first.coerceIn(0, length)
        val endExclusive = (range.last + 1).coerceIn(0, length)
        if (start >= endExclusive) return
        content.delete(start, endExclusive)
        lines.delete(start, endExclusive)
    }

    override fun toOffset(line: Int, col: Int): Int {
        require(line in 0 until lineCount) { "line $line out of bounds" }
        val start = lineStart(line)
        val endExclusive = lineEnd(line)
        val actualCol = col.coerceIn(0, max(0, endExclusive - start))
        return start + actualCol
    }

    override fun toLineCol(offset: Int): Pair<Int, Int> {
        require(offset in 0..length) { "offset $offset out of bounds" }
        val line = lines.lineForOffset(offset)
        val start = lineStart(line)
        return line to (offset - start)
    }

    /**
     * Get full text content as string (for debugging/testing).
     */
    fun snapshot(): String = content.toString()
}

private fun IntRange.isEmpty(): Boolean = last < first
