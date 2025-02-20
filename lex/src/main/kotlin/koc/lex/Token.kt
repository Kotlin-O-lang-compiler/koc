package koc.lex

import koc.utils.Position

sealed class Token(open val value: String, open val kind: TokenKind) {

    constructor(kind: TokenKind) : this(kind.value, kind)
    abstract val start: Position
    
    val end: Position get() = Position(start.line, start.column + value.length.toUInt(), start.filename)

    data class Keyword(override val start: Position, override val kind: TokenKind): Token(kind)
    data class Identifier(override val value: String, override val start: Position): Token(TokenKind.IDENTIFIER)

    data class IntLiteral(val absolute: Long, override val start: Position): Token(absolute.toString(), TokenKind.INT_LITERAL) {
        companion object {
            const val MIN = Long.MIN_VALUE
            const val MAX = Long.MAX_VALUE
        }
    }
    data class RealLiteral(val absolute: Double, override val start: Position): Token(absolute.toString(), TokenKind.REAL_LITERAL)

    data class Special(override val start: Position, override val kind: TokenKind): Token(kind)

    data class Invalid(override val value: String, override val start: Position): Token(value, TokenKind.INVALID)
}