package koc.lex

import koc.utils.Position

sealed class Token(open val value: String, val kind: TokenKind) {

    constructor(kind: TokenKind) : this(kind.value, kind) {
        require(kind != TokenKind.IDENTIFIER && kind != TokenKind.INT_LITERAL && kind != TokenKind.REAL_LITERAL) {
            "Token construction from TokenKind with dynamic value is not possible"
        }
    }
    abstract val start: Position
    
    val end: Position get() = Position(start.line, start.column + value.length.toUInt(), start.filename)

    data class CLASS(override val start: Position): Token(TokenKind.CLASS)
    data class EXTENDS(override val start: Position): Token(TokenKind.EXTENDS)
    data class IS(override val start: Position): Token(TokenKind.IS)
    data class END(override val start: Position): Token(TokenKind.END)
    data class VAR(override val start: Position): Token(TokenKind.VAR)
    data class METHOD(override val start: Position): Token(TokenKind.METHOD)
    data class THIS(override val start: Position): Token(TokenKind.THIS)
    data class WHILE(override val start: Position): Token(TokenKind.WHILE)
    data class LOOP(override val start: Position): Token(TokenKind.LOOP)
    data class IF(override val start: Position): Token(TokenKind.IF)
    data class THEN(override val start: Position): Token(TokenKind.THEN)
    data class ELSE(override val start: Position): Token(TokenKind.ELSE)
    data class RETURN(override val start: Position): Token(TokenKind.RETURN)

    data class IDENTIFIER(override val value: String, override val start: Position): Token(value, TokenKind.IDENTIFIER)

    data class INT_LITERAL(val absolute: Long, override val start: Position): Token(absolute.toString(), TokenKind.INT_LITERAL) {
        companion object {
            const val MIN = Long.MIN_VALUE
            const val MAX = Long.MAX_VALUE
        }
    }
    data class REAL_LITERAL(val absolute: Double, override val start: Position): Token(absolute.toString(), TokenKind.REAL_LITERAL)

    data class TRUE(override val start: Position): Token(true.toString(), TokenKind.TRUE)
    data class FALSE(override val start: Position): Token(false.toString(), TokenKind.FALSE)

    data class DOT(override val start: Position): Token(TokenKind.DOT)
    data class COMMA(override val start: Position): Token(TokenKind.COMMA)
    data class COLON(override val start: Position): Token(TokenKind.COLON)
    data class ASSIGN(override val start: Position): Token(TokenKind.ASSIGN)
    data class WIDE_ARROW(override val start: Position): Token(TokenKind.WIDE_ARROW)
    data class LPAREN(override val start: Position): Token(TokenKind.LPAREN)
    data class RPAREN(override val start: Position): Token(TokenKind.RPAREN)
    data class LSQUARE(override val start: Position): Token(TokenKind.LSQUARE)
    data class RSQUARE(override val start: Position): Token(TokenKind.RSQUARE)

    /**
     * Single-line comment
     */
    data class COMMENT(override val start: Position): Token(TokenKind.COMMENT)

    data class INVALID(override val value: String, override val start: Position): Token(value, TokenKind.INVALID)

}