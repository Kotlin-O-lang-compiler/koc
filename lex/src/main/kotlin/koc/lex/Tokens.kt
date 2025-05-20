package koc.lex

data class Tokens(val tokens: List<Token>) {
    override fun toString(): String {
        return formatTokens(tokens, 0, tokens.lastIndex, showLines = false)
    }
}