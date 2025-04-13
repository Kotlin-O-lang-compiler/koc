package koc.parser

import koc.lex.Token
import koc.lex.TokenKind
import koc.utils.CompileException
import koc.utils.Position

sealed class ParseException(override val message: String) : CompileException(message) {
}

class ExpectedNodeException(val expected: String, val actual: Token, val tokens: List<Token>) : ParseException(
    "Unexpected token '${actual.value}'.\n${formatAsBadToken(actual, tokens, "Expected $expected")}"
)

class LackOfNodeException(val expected: String, val tokens: List<Token>) : ParseException(
    "Premature code end.\n${formatAsBadToken(Token(TokenKind.INVALID, tokens.lastOrNull()?.end.next()), tokens, "Expected $expected")}"
)

class UnexpectedTokenException(
    val actual: Token, val expected: Collection<TokenKind>, val tokens: List<Token>
) : ParseException(
    "Unexpected token '${actual.value}'.\n${
        formatAsBadToken(
            actual,
            tokens,
            "Expected ${expected.joinToString(separator = " or ") { it.diagValue }.ifEmpty { "nothing" }}"
        )
    }"
) {
    init {
        require(expected.isNotEmpty())
    }

    constructor(actual: Token, expected: TokenKind, tokens: List<Token>) : this(actual, listOf(expected), tokens)
}

class LackOfTokenException(
    val expected: Collection<TokenKind>, val tokens: List<Token>
) : ParseException(
    "Premature code end.\n${
        formatAsBadToken(
            Token(expected.firstOrNull() ?: TokenKind.INVALID, tokens.lastOrNull()?.end.next()), 
            tokens,
            "Expected ${expected.joinToString(separator = " or ") { it.diagValue }.ifEmpty { "nothing" }}"
        )
    }"
) {
    init {
        require(expected.isNotEmpty())
    }

    constructor(expected: TokenKind, tokens: List<Token>) : this(listOf(expected), tokens)
}