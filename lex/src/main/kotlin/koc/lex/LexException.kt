package koc.lex

import koc.utils.CompileException
import koc.utils.Position

abstract class LexException(message: String) : CompileException(message)

class UnexpectedTokenException(val actual: String, val expected: List<TokenKind> = emptyList()) : LexException(
    "Unexpected token '$actual'" + when {
        expected.isEmpty() -> ""
        expected.size == 1 -> ", probably you mean '${expected.first().diagValue}'"
        else -> ", probably you mean " + expected.joinToString(", ") { "'${it.diagValue}'" }
    }
)

class IntegerLiteralOutOfRangeException(position: Position, val actual: String) : LexException(
    "Integer literal '$actual' is out of valid range"
)