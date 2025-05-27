package koc.parser

import koc.lex.Token
import koc.lex.TokenKind
import koc.ast.CallExpr
import koc.ast.IntegerLiteral
import koc.ast.InvalidExpr
import koc.ast.MemberAccessExpr
import koc.ast.RealLiteral
import koc.ast.RefExpr
import koc.parser.impl.ParserImpl
import koc.core.Diagnostics
import koc.core.Position
import koc.lex.Tokens
import org.junit.jupiter.api.BeforeEach
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TestParserExpr {

    private val diag = Diagnostics()
    private val parser = ParserImpl(diag)

    @BeforeEach
    fun initialize() {
        diag.clear()
    }

    @Test
    fun `parse this`() {
        val token = Token(TokenKind.THIS, Position(1u, 1u, "test"))
        val tokens = listOf(token)
        val expr1 = parser.parseRefExpr(Tokens(tokens, emptyList()))
        assertFalse { diag.hasErrors }
        assertEquals(token, expr1.identifierToken)
        assertFalse { expr1.isBroken }

        val expr2 = parser.parseExpr(Tokens(tokens, emptyList()))
        assertFalse { diag.hasErrors }
        assertIs<RefExpr>(expr2)
        assertEquals(token, expr2.identifierToken)
        assertFalse { expr2.isBroken }
    }

    @Test
    fun `parse this empty`() {
        val tokens = listOf<Token>()
        val expr1 = parser.parseRefExpr(Tokens(tokens, emptyList()))
        assertTrue { diag.hasErrors }
        assertTrue { expr1.isBroken }

        diag.clear()

        val expr2 = parser.parseExpr(Tokens(tokens, emptyList()))
        assertTrue { diag.hasErrors }
        assertIs<InvalidExpr>(expr2)
        assertTrue { expr2.isBroken }
    }

    @Test
    fun `parse this other tokens`() {
        val tokens = listOf<Token>(
            Token(TokenKind.IS, Position(1u, 1u, "test")),
            Token(TokenKind.END, Position(1u, 4u, "test"))
        )
        val expr1 = parser.parseRefExpr(Tokens(tokens, emptyList()))
        assertTrue { diag.hasErrors }
        assertTrue { expr1.isBroken }

        diag.clear()

        val expr2 = parser.parseExpr(Tokens(tokens, emptyList()))
        assertTrue { diag.hasErrors }
        assertIs<InvalidExpr>(expr2)
        assertTrue { expr2.isBroken }
    }


    @Test
    fun `parse int literal`() {
        val value = -123L
        val token = Token(value.toString(), TokenKind.INT_LITERAL, Position(1u, 1u, "test"))
        val tokens = listOf(token)
        val expr1 = parser.parseIntegerLiteral(Tokens(tokens, emptyList()))
        assertFalse { diag.hasErrors }
        assertEquals(value, expr1.value)
        assertEquals(token, expr1.token)
        assertFalse { expr1.isBroken }
        val expr2 = parser.parseExpr(Tokens(tokens, emptyList()))
        assertFalse { diag.hasErrors }
        assertIs<IntegerLiteral>(expr2)
        assertEquals(value, expr2.value)
        assertEquals(token, expr2.token)
        assertFalse { expr2.isBroken }
    }

    @Test
    fun `parse int literal bad`() {
        val tokens = listOf<Token>(
            Token(TokenKind.IS, Position(1u, 1u, "test")),
            Token(TokenKind.END, Position(1u, 4u, "test"))
        )
        val expr = parser.parseIntegerLiteral(Tokens(tokens, emptyList()))
        assertTrue { diag.hasErrors }
        assertTrue { expr.isBroken }
    }

    @Test
    fun `parse real literal`() {
        val value = -0.125
        val token = Token(value.toString(), TokenKind.REAL_LITERAL, Position(1u, 1u, "test"))
        val tokens = listOf(token)
        val expr1 = parser.parseRealLiteral(Tokens(tokens, emptyList()))
        assertFalse { diag.hasErrors }
        assertEquals(value, expr1.value)
        assertEquals(token, expr1.token)
        assertFalse { expr1.isBroken }
        val expr2 = parser.parseExpr(Tokens(tokens, emptyList()))
        assertFalse { diag.hasErrors }
        assertIs<RealLiteral>(expr2)
        assertEquals(value, expr2.value)
        assertEquals(token, expr2.token)
        assertFalse { expr2.isBroken }
    }

    @Test
    fun `parse real literal bad`() {
        val tokens = listOf<Token>(
            Token(TokenKind.IS, Position(1u, 1u, "test")),
            Token(TokenKind.END, Position(1u, 4u, "test"))
        )
        val expr = parser.parseRealLiteral(Tokens(tokens, emptyList()))
        assertTrue { diag.hasErrors }
        assertTrue { expr.isBroken }
    }

    @Test
    fun `parse call without args`() {
        val tokens = listOf(
            Token("foo", TokenKind.IDENTIFIER, Position(1u, 1u, "test")),
            Token(TokenKind.LPAREN, Position(1u, 5u, "test")),
            Token(TokenKind.RPAREN, Position(1u, 6u, "test"))
        )
        val expr = parser.parseExpr(Tokens(tokens, emptyList()))
        assertIs<CallExpr>(expr)
        assertFalse { diag.hasErrors }
        assertFalse { expr.isBroken }
        assertTrue { expr.args.isEmpty() }
    }

    @Test
    fun `parse call with args`() {
        val tokens = listOf(
            Token("foo", TokenKind.IDENTIFIER, Position(1u, 1u, "test")),
            Token(TokenKind.LPAREN, Position(1u, 5u, "test")),
            Token("arg", TokenKind.IDENTIFIER, Position(1u, 6u, "test")),
            Token(TokenKind.COMMA, Position(1u, 9u, "test")),
            Token("5", TokenKind.INT_LITERAL, Position(1u, 11u, "test")),
            Token(TokenKind.RPAREN, Position(1u, 13u, "test"))
        )
        val expr = parser.parseExpr(Tokens(tokens, emptyList()))
        assertIs<CallExpr>(expr)
        assertFalse { diag.hasErrors }
        assertFalse { expr.isBroken }
        assertEquals(2, expr.args.size)
        assertNull(expr.args[0].commaToken)
        assertIs<RefExpr>(expr.args[0].expr)

        assertNotNull(expr.args[1].commaToken)
        assertIs<IntegerLiteral>(expr.args[1].expr)
    }

    @Test
    fun `parse member call without args`() {
        val tokens = listOf(
            Token("obj", TokenKind.IDENTIFIER, Position(1u, 1u, "test")),
            Token(TokenKind.DOT, Position(1u, 5u, "test")),
            Token("foo", TokenKind.IDENTIFIER, Position(1u, 1u, "test")),
            Token(TokenKind.LPAREN, Position(1u, 5u, "test")),
            Token(TokenKind.RPAREN, Position(1u, 6u, "test"))
        )
        val access = parser.parseExpr(Tokens(tokens, emptyList()))
        assertFalse { diag.hasErrors }
        assertFalse { access.isBroken }

        assertIs<MemberAccessExpr>(access)
        assertEquals(tokens.first(), (access.left as RefExpr).identifierToken)
        assertEquals(tokens[1], access.dot)

        val call = access.member
        assertIs<CallExpr>(call)
        assertFalse { call.isBroken }

        assertTrue { call.args.isEmpty() }
    }
}