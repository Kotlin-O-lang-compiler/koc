package koc.lex

import kotlin.math.max
import kotlin.math.min

data class Window(val start: Int, val end: Int, val allTokens: List<Token>) {
    constructor(start: Token, end: Token, tokens: List<Token>) : this(
        if (start.isValid) tokens.indexOf(start) else -1,
        if (start.isValid) tokens.indexOf(end) else -1,
        tokens
    )

    init {
        require(start <= end)
    }

    val endExclusive: Int
        get() = end + 1

    val startToken: Token get() = allTokens[start]
    val endToken: Token get() = allTokens[end]

    override fun toString(): String = formatTokens(allTokens, start, end, onlyWindow = true, showLines = false).trimIndent()

    val tokens: List<Token>
        get() = if(start == -1 || end == -1) emptyList() else allTokens.subList(start, endExclusive)

    fun areSameTokens(other: Window): Boolean = allTokens == other.allTokens

    operator fun plus(other: Window?): Window {
        if (other == null) return this
        check(areSameTokens(other))
        val extStart = min(start, other.start)
        val extEnd = max(end, other.end)
        return Window(extStart, extEnd, allTokens)
    }
}