package koc.parser.diag

import koc.lex.Token
import koc.lex.TokenKind
import koc.parser.UnexpectedTokenException
import koc.utils.Position
import kotlin.test.Test

class TestDiag {
    @Test
    fun testDiagTokenUnderlineToken() {
        val token = Token(TokenKind.CLASS, Position(1u, 1u, "file"))
        val expected = Token(TokenKind.LPAREN, Position.fake)
        val msg = UnexpectedTokenException(token, expected.kind, listOf(token)).message
        println(msg)
    }

    @Test
    fun testDiagTokenUnderlineTokens() {
        val bad = Token(TokenKind.CLASS, Position(1u, 8u, "file"))
        val tokens = listOf(
            Token(TokenKind.CLASS, Position(1u, 1u, "file")),
            bad,
            Token("MyClass", TokenKind.IDENTIFIER, Position(1u, 14u, "file"))
        )

        val expected = Token(TokenKind.IDENTIFIER, Position.fake)
        val msg = UnexpectedTokenException(bad, expected.kind, tokens).message
        println(msg)
    }

    @Test
    fun testDiagTokenUnderlineTokensMultiline() {
        val bad = Token(TokenKind.CLASS, Position(3u, 4u, "file"))
        val tokens = listOf(
            Token(TokenKind.CLASS, Position(1u, 1u, "file")),
            Token("MyClass", TokenKind.IDENTIFIER, Position(1u, 14u, "file")),
            Token(TokenKind.IS, Position(2u, 4u, "file")),
            bad,
            Token(TokenKind.END, Position(4u, 1u, "file"))
        )

        val expected = Token(TokenKind.IDENTIFIER, Position.fake)
        val msg = UnexpectedTokenException(bad, expected.kind, tokens).message
        println(msg)
    }
}