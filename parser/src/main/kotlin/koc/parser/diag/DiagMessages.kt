package koc.parser.diag

import koc.core.DiagMessage
import koc.lex.Token
import koc.lex.TokenKind

class LackOfToken(val expected: Collection<TokenKind>, override val code: List<String>) : DiagMessage(LackOfTokenKind) {
    constructor(expected: TokenKind, code: List<String>) : this(listOf(expected), code)

    override fun toString(): String = "Premature code end"

    override val extraMessage: String?
        get() = "Expected ${expected.joinToString(separator = " or ") { it.diagValue }.ifEmpty { "nothing" }}"
}

class LackOfNode(val expected: Collection<String>, override val code: List<String>) : DiagMessage(LackOfNodeKind) {
    override fun toString(): String = "Premature code end"

    override val extraMessage: String?
        get() = "Expected ${expected.joinToString(separator = " or ") { it }.ifEmpty { "nothing" }}"
}

class UnexpectedToken(val actual: Token, val expected: Collection<TokenKind>, override val code: List<String>) :
    DiagMessage(UnexpectedTokenKind) {
    constructor(actual: Token, expected: TokenKind, code: List<String>) : this(actual, listOf(expected), code)

    override fun toString(): String = "Unexpected token '${actual.value}'" + when {
        expected.isEmpty() -> ""
        else -> ", probably you mean " + expected.joinToString(", ") { it.diagValue }
    }
}

class OtherNodeExpected(val expected: Collection<String>, override val code: List<String>) :
    DiagMessage(OtherNodeExpectedKind) {
    override fun toString(): String = "Unexpected tokens. Expected " + when {
        expected.isEmpty() -> throw IllegalArgumentException("expected nodes should not be empty")
        else -> expected.joinToString(", ") { it }
    }
}