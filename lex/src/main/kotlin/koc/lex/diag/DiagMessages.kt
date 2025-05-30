package koc.lex.diag

import koc.core.DiagMessage
import koc.lex.TokenKind

class IntegerLiteralOutOfRange(val actual: String, override val code: List<String>) :
    DiagMessage(IntegerLiteralValueOutOfBoundsKind) {
    override fun toString(): String = "Integer literal '$actual' is out of valid range"
}

class UnexpectedToken(
    val actual: String, val expected: List<TokenKind> = emptyList(),
    override val code: List<String>
) : DiagMessage(UnexpectedTokenKind) {
    override fun toString(): String = "Unexpected token '$actual'" + when {
        expected.isEmpty() -> ""
        else -> ", probably you mean " + expected.joinToString(", ") { it.diagValue }
    }
}