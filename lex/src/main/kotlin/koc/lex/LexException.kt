package koc.lex

import koc.utils.CompileException
import koc.utils.Position

abstract class LexException(val position: Position, message: String) : CompileException(message)

class UnexpectedTokenException(position: Position, val actual: String, val expected: List<TokenKind> = emptyList()) : LexException(
    position,
    "Unexpected token '$actual' at position $position" + when {
        expected.isEmpty() -> ""
        expected.size == 1 -> ", probably you mean '${expected.first().diagValue}'"
        else -> ", probably you mean " + expected.joinToString(", ") { "'${it.diagValue}'" }
    }
)

class IntegerLiteralOutOfRangeException(position: Position, val actual: String) : LexException(
    position,
    "Integer literal '$actual' is out of valid range"
    )