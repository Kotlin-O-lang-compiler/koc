package koc.parser.diag

import koc.core.DiagMessage
import koc.lex.Token
import koc.lex.TokenKind

class LackOfToken(val expected: Collection<TokenKind>) : DiagMessage(LackOfTokenKind) {
    constructor(expected: TokenKind) : this(listOf(expected))

    override fun toString(): String = "Premature code end"

    override val extraMessage: String?
        get() = "Expected ${expected.joinToString(separator = " or ") { it.diagValue }.ifEmpty { "nothing" }}"
}

class LackOfNode(val expected: Collection<String>) : DiagMessage(LackOfNodeKind) {
    override fun toString(): String = "Premature code end"

    override val extraMessage: String?
        get() = "Expected ${expected.joinToString(separator = " or ") { it }.ifEmpty { "nothing" }}"
}

class UnexpectedToken(val actual: Token, val expected: Collection<TokenKind>) : DiagMessage(UnexpectedTokenKind) {
    constructor(actual: Token, expected: TokenKind) : this(actual, listOf(expected))

    override fun toString(): String = "Unexpected token '${actual.value}'" + when {
        expected.isEmpty() -> ""
        else -> ", probably you mean " + expected.joinToString(", ") { it.diagValue }
    }
}

class OtherNodeExpected(val expected: Collection<String>) : DiagMessage(OtherNodeExpectedKind) {
    override fun toString(): String = "Unexpected tokens. Expected " + when {
        expected.isEmpty() -> throw IllegalArgumentException("expected nodes should not be empty")
        else -> expected.joinToString(", ") { it }
    }
}