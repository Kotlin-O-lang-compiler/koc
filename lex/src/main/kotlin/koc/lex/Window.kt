package koc.lex

import kotlin.math.max
import kotlin.math.min

data class Window(val start: Int, val end: Int, val allTokens: Tokens) {
    constructor(start: Token, end: Token, tokens: Tokens) : this(
        if (start.isValid) tokens.tokens.indexOf(start) else -1,
        if (start.isValid) tokens.tokens.indexOf(end) else -1,
        tokens
    )

    init {
        require(start <= end)
    }

    val endExclusive: Int
        get() = end + 1

    val startToken: Token get() = allTokens.tokens[start]
    val endToken: Token get() = allTokens.tokens[start]

    override fun toString(): String = formatTokens(allTokens.tokens, start, end)

    val tokens: List<Token>
        get() = if(start == -1 || end == -1) emptyList() else allTokens.tokens.subList(start, endExclusive)

    fun areSameTokens(other: Window): Boolean = allTokens == other.allTokens

    operator fun plus(other: Window): Window {
        check(areSameTokens(other))
        val extStart = min(start, other.start)
        val extEnd = max(end, other.end)
        return Window(extStart, extEnd, allTokens)
    }
}