package ai.codeeditor.core

import kotlin.math.max

/**
 * LineIndex maintains line start offsets for fast offset ↔ (line, col) conversion.
 * Uses binary search for lookups and incrementally updates on insert/delete.
 */
class LineIndex private constructor(
    private val starts: MutableList<Int>,
    private var totalLength: Int
) {
    val count: Int
        get() = starts.size

    fun lineStart(line: Int): Int {
        require(line in 0 until count) { "line $line out of bounds" }
        return starts[line]
    }

    fun nextLineStart(line: Int): Int {
        require(line in 0 until count) { "line $line out of bounds" }
        return if (line + 1 < count) starts[line + 1] else totalLength
    }

    /**
     * Binary search to find which line contains the given offset.
     */
    fun lineForOffset(offset: Int): Int {
        require(offset in 0..totalLength) { "offset $offset out of bounds" }
        var low = 0
        var high = starts.size
        while (low < high) {
            val mid = (low + high) / 2
            if (starts[mid] <= offset) {
                low = mid + 1
            } else {
                high = mid
            }
        }
        return (low - 1).coerceAtLeast(0)
    }

    /**
     * Insert text at offset, updating line starts for any newlines in the text.
     */
    fun insert(offset: Int, text: CharSequence) {
        if (text.isEmpty()) return
        val delta = text.length
        val insertPos = insertionIndex(offset)
        val newStarts = collectNewLineStarts(text, offset)
        starts.addAll(insertPos, newStarts)
        adjustStarts(insertPos + newStarts.size, delta)
        totalLength += delta
    }

    /**
     * Delete range [startOffset, endOffsetExclusive), removing affected line starts.
     */
    fun delete(startOffset: Int, endOffsetExclusive: Int) {
        if (startOffset >= endOffsetExclusive) return
        val delta = endOffsetExclusive - startOffset
        removeLineStarts(startOffset, endOffsetExclusive)
        adjustStarts(insertionIndex(endOffsetExclusive), -delta)
        totalLength -= delta
    }

    private fun insertionIndex(offset: Int): Int {
        var low = 0
        var high = starts.size
        while (low < high) {
            val mid = (low + high) / 2
            if (starts[mid] <= offset) {
                low = mid + 1
            } else {
                high = mid
            }
        }
        return low
    }

    private fun collectNewLineStarts(text: CharSequence, absoluteOffset: Int): List<Int> {
        val list = ArrayList<Int>()
        text.forEachIndexed { index, c ->
            if (c == '\n') {
                list += absoluteOffset + index + 1
            }
        }
        return list
    }

    private fun adjustStarts(fromIndex: Int, delta: Int) {
        for (i in fromIndex until starts.size) {
            starts[i] += delta
        }
    }

    private fun removeLineStarts(startOffset: Int, endOffsetExclusive: Int) {
        val iterator = starts.listIterator(1) // keep first entry (0)
        while (iterator.hasNext()) {
            val value = iterator.next()
            if (value in startOffset + 1 until endOffsetExclusive + 1) {
                iterator.remove()
            }
        }
    }

    companion object {
        fun fromText(text: CharSequence): LineIndex {
            val starts = mutableListOf(0)
            text.forEachIndexed { index, c ->
                if (c == '\n') {
                    starts += index + 1
                }
            }
            return LineIndex(starts, text.length)
        }
    }
}
