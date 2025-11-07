package ai.codeeditor.syntax

/**
 * Cache for tokenized lines with automatic invalidation.
 * Only tokenizes lines when needed (viewport-based rendering).
 */
class TokenCache(
    private val tokenizer: KotlinTokenizer = KotlinTokenizer()
) {
    private val cache = mutableMapOf<Int, LineTokens>()
    private val maxCacheSize = 10000
    
    /**
     * Get tokens for a line, using cache if valid.
     */
    fun getTokens(lineNumber: Int, lineText: String): LineTokens {
        val lineHash = lineText.hashCode()
        val cached = cache[lineNumber]
        
        // Return cached if valid
        if (cached != null && cached.lineHash == lineHash) {
            return cached
        }
        
        // Tokenize and cache
        val tokens = tokenizer.tokenize(lineText, lineNumber)
        cache[lineNumber] = tokens
        
        // Limit cache size (simple FIFO)
        if (cache.size > maxCacheSize) {
            val oldestKey = cache.keys.minOrNull()
            if (oldestKey != null) {
                cache.remove(oldestKey)
            }
        }
        
        return tokens
    }
    
    /**
     * Invalidate a specific line (called when line is edited).
     */
    fun invalidateLine(lineNumber: Int) {
        cache.remove(lineNumber)
    }
    
    /**
     * Invalidate a range of lines (called when lines are inserted/deleted).
     */
    fun invalidateRange(startLine: Int, endLine: Int) {
        for (line in startLine..endLine) {
            cache.remove(line)
        }
    }
    
    /**
     * Shift line numbers (called when lines are inserted/deleted).
     * Lines after insertPoint need their cache keys updated.
     */
    fun shiftLines(insertPoint: Int, delta: Int) {
        if (delta == 0) return
        
        val affectedEntries = cache.entries
            .filter { it.key >= insertPoint }
            .sortedByDescending { it.key }
        
        // Remove old entries
        affectedEntries.forEach { cache.remove(it.key) }
        
        // Re-add with new line numbers
        affectedEntries.forEach { (oldLine, tokens) ->
            val newLine = oldLine + delta
            if (newLine >= 0) {
                cache[newLine] = tokens.copy(line = newLine)
            }
        }
    }
    
    /**
     * Clear entire cache (useful when switching files or major changes).
     */
    fun clear() {
        cache.clear()
    }
    
    /**
     * Get cache statistics for debugging.
     */
    fun getStats(): CacheStats {
        return CacheStats(
            size = cache.size,
            maxSize = maxCacheSize
        )
    }
}

/**
 * Cache statistics for monitoring.
 */
data class CacheStats(
    val size: Int,
    val maxSize: Int
)
