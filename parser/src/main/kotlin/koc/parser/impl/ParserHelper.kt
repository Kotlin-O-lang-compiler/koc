package koc.parser.impl

import koc.lex.Token
import koc.lex.TokenKind
import koc.parser.LackOfTokenException
import koc.parser.nextPosition
import koc.utils.Diagnostics

abstract class ParserHelper(private val tokens: List<Token>, private val diag: Diagnostics) {
    private var idx = 0

    val current: Token? get() = if (tokens.size > idx) tokens[idx] else null
    val next: Token? get() = if (tokens.size > idx + 1) tokens[idx + 1] else null

    private var isBad = false

    fun expect(tokenKind: TokenKind): Token {
        val nxt = next
        if (nxt == null) {
            isBad = true
            diag.error(LackOfTokenException(tokenKind, tokens), tokens.nextPosition)
            return Token.invalid
        }

        idx++
        return nxt
    }
}