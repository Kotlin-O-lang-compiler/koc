package koc.parser

import koc.lex.Token
import koc.lex.TokenKind
import koc.utils.CompileException

sealed class ParseException(override val message: String): CompileException(message) {
}

class UnexpectedTokenException(
    val actual: Token, val expected: TokenKind, val tokens: List<Token>
): ParseException("Unexpected token '${actual.value}' met.\n${formatAsBadToken(actual, tokens, "Expected ${expected.diagValue}")}") {
}

class LackOfTokenException(
    val expected: TokenKind, val tokens: List<Token>
) : ParseException("Premature code end.\n${formatAsBadToken(Token(expected, tokens.last().end.copy(column = UInt.MAX_VALUE)), tokens, "Expected ${expected.diagValue}")}")