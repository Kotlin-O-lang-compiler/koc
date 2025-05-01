package koc.parser

import koc.lex.Token
import koc.lex.TokenKind
import koc.parser.ast.FieldDecl
import koc.parser.ast.IntegerLiteral
import koc.parser.ast.InvalidExpr
import koc.parser.ast.MethodDecl
import koc.parser.ast.RealLiteral
import koc.parser.impl.ParserImpl
import koc.utils.Diagnostics
import koc.utils.Position
import org.junit.jupiter.api.BeforeEach
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class TestParserClass {

    private val diag = Diagnostics()
    private val parser = ParserImpl(diag)

    @BeforeEach
    fun initialize() {
        diag.clear()
    }

    @Test
    fun `parse empty class`() {
        val tokens = listOf(
            Token(TokenKind.CLASS, Position(1u, 1u, "test")),
            Token("k", TokenKind.IDENTIFIER, Position(1u, 7u, "test")),
            Token(TokenKind.IS, Position(1u, 10u, "test")),
            Token(TokenKind.END, Position(1u, 13u, "test"))
        )
        val classDecl = parser.parseClassDecl(tokens)
        assertFalse { classDecl.isBroken }
        assertFalse { diag.hasErrors }
        assertEquals(tokens[0], classDecl.classToken)
        assertEquals(tokens[1], classDecl.identifierToken)
        assertEquals(tokens[1].value, classDecl.identifier.value)
        assertEquals(tokens[2], classDecl.body.isToken)
        assertEquals(tokens[3], classDecl.body.endToken)
    }

    @Test
    fun `parse class with field`() {
        val tokens = listOf(
            Token(TokenKind.CLASS, Position(1u, 1u, "test")),
            Token("k", TokenKind.IDENTIFIER, Position(1u, 7u, "test")),
            Token(TokenKind.IS, Position(1u, 9u, "test")),
            Token(TokenKind.VAR, Position(2u, 4u, "test")),
            Token("field", TokenKind.IDENTIFIER, Position(2u, 8u, "test")),
            Token(TokenKind.COLON, Position(2u, 14u, "test")),
            Token("0", TokenKind.INT_LITERAL, Position(2u, 16u, "test")),

            Token(TokenKind.END, Position(3u, 13u, "test"))
        )
        val classDecl = parser.parseClassDecl(tokens)
        assertFalse { classDecl.isBroken }
        assertFalse { diag.hasErrors }
        assertEquals(tokens[0], classDecl.classToken)
        assertEquals(tokens[1], classDecl.identifierToken)
        assertEquals(tokens[1].value, classDecl.identifier.value)
        assertEquals(tokens[2], classDecl.body.isToken)
        assertEquals(tokens[7], classDecl.body.endToken)

        assertEquals(1, classDecl.body.members.size)
        val field = classDecl.body.members.first()

        assertIs<FieldDecl>(field)
        assertEquals(classDecl, field.outerDecl)
        assertEquals(tokens[3], field.varDecl.keyword)
        assertEquals(tokens[4], field.varDecl.identifierToken)
        assertEquals(tokens[5], field.varDecl.colonToken)

        val intlit = field.varDecl.initializer
        assertIs<IntegerLiteral>(intlit)
        assertEquals(tokens[6], intlit.token)
        assertEquals(tokens[6].value.toLong(), intlit.value)
    }

    @Test
    fun `parse class with two fields`() {
        val varToken = listOf(
            Token(TokenKind.VAR, Position(2u, 4u, "test")),
            Token("field", TokenKind.IDENTIFIER, Position(2u, 8u, "test")),
            Token(TokenKind.COLON, Position(2u, 14u, "test")),
            Token("0", TokenKind.INT_LITERAL, Position(2u, 16u, "test"))
        )
        val tokens = listOf(
            Token(TokenKind.CLASS, Position(1u, 1u, "test")),
            Token("k", TokenKind.IDENTIFIER, Position(1u, 7u, "test")),
            Token(TokenKind.IS, Position(1u, 9u, "test")),
            *varToken.toTypedArray(),
            *varToken.toTypedArray(),
            Token(TokenKind.END, Position(3u, 13u, "test"))
        )
        val classDecl = parser.parseClassDecl(tokens)
        assertFalse { classDecl.isBroken }
        assertFalse { diag.hasErrors }
        assertEquals(tokens[0], classDecl.classToken)
        assertEquals(tokens[1], classDecl.identifierToken)
        assertEquals(tokens[1].value, classDecl.identifier.value)
        assertEquals(tokens[2], classDecl.body.isToken)
        assertEquals(tokens[11], classDecl.body.endToken)

        assertEquals(2, classDecl.body.members.size)

        for (field in classDecl.body.members) {
            assertIs<FieldDecl>(field)
            assertEquals(classDecl, field.outerDecl)
            assertEquals(varToken[0], field.varDecl.keyword)
            assertEquals(varToken[1], field.varDecl.identifierToken)
            assertEquals(varToken[2], field.varDecl.colonToken)

            val intlit = field.varDecl.initializer
            assertIs<IntegerLiteral>(intlit)
            assertEquals(varToken[3], intlit.token)
            assertEquals(varToken[3].value.toLong(), intlit.value)
        }
    }

    @Test
    fun `parse class with forward method decl easy`() {
        val methodTokens = listOf(
            Token(TokenKind.METHOD, Position(2u, 1u, "test")),
            Token("foo", TokenKind.IDENTIFIER, Position(2u, 7u, "test")),

        )
        val tokens = listOf(
            Token(TokenKind.CLASS, Position(1u, 1u, "test")),
            Token("k", TokenKind.IDENTIFIER, Position(1u, 7u, "test")),
            Token(TokenKind.IS, Position(1u, 9u, "test")),
            *methodTokens.toTypedArray(),
            Token(TokenKind.END, Position(3u, 13u, "test"))
        )
        val classDecl = parser.parseClassDecl(tokens)
        assertFalse { classDecl.isBroken }
        assertFalse { diag.hasErrors }
        assertEquals(tokens[0], classDecl.classToken)
        assertEquals(tokens[1], classDecl.identifierToken)
        assertEquals(tokens[1].value, classDecl.identifier.value)
        assertEquals(tokens[2], classDecl.body.isToken)
        assertEquals(tokens.last(), classDecl.body.endToken)

        assertEquals(1, classDecl.body.members.size)
        val method = classDecl.body.members.first()

        assertIs<MethodDecl>(method)
        assertEquals(classDecl, method.outerDecl)
        assertEquals(methodTokens[0], method.keyword)
        assertEquals(methodTokens[1], method.identifierToken)
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