package koc.parser

import koc.lex.Token
import koc.lex.TokenKind
import koc.parser.ast.IntegerLiteral
import koc.parser.ast.InvalidExpr
import koc.parser.ast.RealLiteral
import koc.parser.ast.ThisExpr
import koc.parser.impl.ParserImpl
import koc.utils.Diagnostics
import koc.utils.Position
import org.junit.jupiter.api.BeforeEach
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
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
        val expr1 = parser.parseThisExpr(tokens)
        assertFalse { diag.hasErrors }
        assertEquals(token, expr1.token)
        assertFalse { expr1.isBroken }

        val expr2 = parser.parseExpr(tokens)
        assertFalse { diag.hasErrors }
        assertIs<ThisExpr>(expr2)
        assertEquals(token, expr2.token)
        assertFalse { expr2.isBroken }
    }

    @Test
    fun `parse this empty`() {
        val tokens = listOf<Token>()
        val expr1 = parser.parseThisExpr(tokens)
        assertTrue { diag.hasErrors }
        assertTrue { expr1.isBroken }

        diag.clear()

        val expr2 = parser.parseExpr(tokens)
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
        val expr1 = parser.parseThisExpr(tokens)
        assertTrue { diag.hasErrors }
        assertTrue { expr1.isBroken }

        diag.clear()

        val expr2 = parser.parseExpr(tokens)
        assertTrue { diag.hasErrors }
        assertIs<InvalidExpr>(expr2)
        assertTrue { expr2.isBroken }
    }


    @Test
    fun `parse int literal`() {
        val value = -123L
        val token = Token(value.toString(), TokenKind.INT_LITERAL, Position(1u, 1u, "test"))
        val tokens = listOf(token)
        val expr1 = parser.parseIntegerLiteral(tokens)
        assertFalse { diag.hasErrors }
        assertEquals(value, expr1.value)
        assertEquals(token, expr1.token)
        assertFalse { expr1.isBroken }
        val expr2 = parser.parseExpr(tokens)
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
        val expr = parser.parseIntegerLiteral(tokens)
        assertTrue { diag.hasErrors }
        assertTrue { expr.isBroken }
    }

    @Test
    fun `parse real literal`() {
        val value = -0.125
        val token = Token(value.toString(), TokenKind.REAL_LITERAL, Position(1u, 1u, "test"))
        val tokens = listOf(token)
        val expr1 = parser.parseRealLiteral(tokens)
        assertFalse { diag.hasErrors }
        assertEquals(value, expr1.value)
        assertEquals(token, expr1.token)
        assertFalse { expr1.isBroken }
        val expr2 = parser.parseExpr(tokens)
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
        val expr = parser.parseRealLiteral(tokens)
        assertTrue { diag.hasErrors }
        assertTrue { expr.isBroken }
    }
}