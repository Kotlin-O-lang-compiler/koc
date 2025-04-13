package koc.lex

import koc.utils.Position

data class Token(val value: String, val kind: TokenKind, val start: Position) {

    constructor(kind: TokenKind, start: Position) : this(kind.value, kind, start)
    
    val end: Position get() = Position(start.line, start.column + length.toUInt() - 1u, start.filename)

    val isValid: Boolean get() = kind != TokenKind.INVALID

    val length: Int get() = value.length

    companion object {
        const val INT_MIN = Long.MIN_VALUE
        const val INT_MAX = Long.MAX_VALUE

        val invalid: Token get() = Token(TokenKind.INVALID, Position.fake)
    }
}