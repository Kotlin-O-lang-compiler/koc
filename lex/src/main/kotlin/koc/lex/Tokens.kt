package koc.lex

class Tokens(val tokens: List<Token>, readCode: () -> List<String>) {
    constructor(tokens: List<Token>, code: List<String>) : this(tokens, { code })

    val code: List<String> by lazy { readCode() }

    override fun toString(): String {
        return formatTokens(tokens, 0, tokens.lastIndex, showLines = false)
    }

    val size: Int get() = tokens.size
    operator fun get(index: Int) = tokens[index]
    val indices: IntRange get() = tokens.indices
    val first: Token get() = tokens.first()
    val last: Token get() = tokens.last()

    fun first() = tokens.first()
    fun last() = tokens.last()
}