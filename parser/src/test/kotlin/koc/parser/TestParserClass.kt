package koc.parser

import koc.lex.Token
import koc.lex.TokenKind
import koc.ast.BooleanLiteral
import koc.ast.FieldDecl
import koc.ast.IntegerLiteral
import koc.ast.MethodBody
import koc.ast.MethodDecl
import koc.ast.VarDecl
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
        val classDecl = parser.parseClassDecl(Tokens(tokens, emptyList()))
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
        val classDecl = parser.parseClassDecl(Tokens(tokens, emptyList()))
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
        val classDecl = parser.parseClassDecl(Tokens(tokens, emptyList()))
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
    fun `parse class with forward method decl min`() {
        val methodTokens = listOf(
            Token(TokenKind.METHOD, Position(2u, 1u, "test")),
            Token("foo", TokenKind.IDENTIFIER, Position(2u, 8u, "test"))
        )
        val tokens = listOf(
            Token(TokenKind.CLASS, Position(1u, 1u, "test")),
            Token("k", TokenKind.IDENTIFIER, Position(1u, 7u, "test")),
            Token(TokenKind.IS, Position(1u, 9u, "test")),
            *methodTokens.toTypedArray(),
            Token(TokenKind.END, Position(3u, 13u, "test"))
        )
        val classDecl = parser.parseClassDecl(Tokens(tokens, emptyList()))
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
    fun `parse method forward decl min`() {
        val tokens = listOf(
            Token(TokenKind.METHOD, Position(2u, 1u, "test")),
            Token("foo", TokenKind.IDENTIFIER, Position(2u, 8u, "test"))
        )
        val method = parser.parseMethod(Tokens(tokens, emptyList()))
        assertFalse { method.isBroken }
        assertFalse { diag.hasErrors }

        assertTrue { method.isForwardDecl }
        assertEquals(tokens[0], method.keyword)
        assertEquals(tokens[1], method.identifierToken)
    }

    @Test
    fun `parse method forward decl with empty params`() {
        val tokens = listOf(
            Token(TokenKind.METHOD, Position(2u, 1u, "test")),
            Token("foo", TokenKind.IDENTIFIER, Position(2u, 8u, "test")),
            Token(TokenKind.LPAREN, Position(2u, 12u, "test")),
            Token(TokenKind.RPAREN, Position(2u, 13u, "test")),
        )
        val method = parser.parseMethod(Tokens(tokens, emptyList()))
        assertFalse { method.isBroken }
        assertFalse { diag.hasErrors }

        assertTrue { method.isForwardDecl }
        assertEquals(tokens[0], method.keyword)
        assertEquals(tokens[1], method.identifierToken)
        assertNotNull(method.params)
        assertTrue { method.params!!.params.isEmpty() }
    }

    @Test
    fun `parse method forward decl with ret type`() {
        val tokens = listOf(
            Token(TokenKind.METHOD, Position(2u, 1u, "test")),
            Token("foo", TokenKind.IDENTIFIER, Position(2u, 8u, "test")),
            Token(TokenKind.COLON, Position(2u, 12u, "test")),
            Token("Integer", TokenKind.IDENTIFIER, Position(2u, 14u, "test")),
        )
        val method = parser.parseMethod(Tokens(tokens, emptyList()))
        assertFalse { method.isBroken }
        assertFalse { diag.hasErrors }

        assertTrue { method.isForwardDecl }
        assertEquals(tokens[0], method.keyword)
        assertEquals(tokens[1], method.identifierToken)
        assertNull(method.params)
        assertNotNull(method.colon)
        assertNotNull(method.retTypeRef)
        assertFalse { method.retTypeRef!!.isBroken }
        assertEquals(tokens.last(), method.retTypeRef!!.identifierToken)
    }

    @Test
    fun `parse method with empty body`() {
        val tokens = listOf(
            Token(TokenKind.METHOD, Position(2u, 1u, "test")),
            Token("foo", TokenKind.IDENTIFIER, Position(2u, 8u, "test")),
            Token(TokenKind.IS, Position(2u, 12u, "test")),
            Token(TokenKind.END, Position(2u, 15u, "test")),
        )
        val method = parser.parseMethod(Tokens(tokens, emptyList()))
        assertFalse { method.isBroken }
        assertFalse { diag.hasErrors }

        assertFalse { method.isForwardDecl }
        assertEquals(tokens[0], method.keyword)
        assertEquals(tokens[1], method.identifierToken)
        assertNull(method.params)
        assertNull(method.colon)
        assertNull(method.retTypeRef)
        assertNotNull(method.body)
        assertFalse { method.body!!.isBroken }
        assertIs<MethodBody.MBody>(method.body!!)
        assertTrue { (method.body!! as MethodBody.MBody).body.nodes.isEmpty() }
    }

    @Test
    fun `parse method arrow body`() {
        val tokens = listOf(
            Token(TokenKind.METHOD, Position(2u, 1u, "test")),
            Token("foo", TokenKind.IDENTIFIER, Position(2u, 8u, "test")),
            Token(TokenKind.COLON, Position(2u, 12u, "test")),
            Token("Integer", TokenKind.IDENTIFIER, Position(2u, 14u, "test")),
            Token(TokenKind.WIDE_ARROW, Position(2u, 22u, "test")),
            Token("5", TokenKind.INT_LITERAL, Position(2u, 25u, "test")),
        )
        val method = parser.parseMethod(Tokens(tokens, emptyList()))
        assertFalse { method.isBroken }
        assertFalse { diag.hasErrors }

        assertFalse { method.isForwardDecl }
        assertEquals(tokens[0], method.keyword)
        assertEquals(tokens[1], method.identifierToken)
        assertNull(method.params)
        assertNotNull(method.colon)
        assertNotNull(method.retTypeRef)
        assertEquals(tokens[3], method.retTypeRef!!.identifierToken)
        assertNotNull(method.body)
        assertFalse { method.body!!.isBroken }

        val body = method.body
        assertIs<MethodBody.MExpr>(body)
        assertIs<IntegerLiteral>(body.expr)
        assertEquals(tokens.last().value.toLong(), (body.expr as IntegerLiteral).value)
    }

    @Test
    fun `parse constructor with empty body`() {
        val tokens = listOf(
            Token(TokenKind.THIS, Position(2u, 1u, "test")),
            Token(TokenKind.IS, Position(2u, 12u, "test")),
            Token(TokenKind.END, Position(2u, 15u, "test")),
        )
        val ctor = parser.parseConstructor(Tokens(tokens, emptyList()))
        assertFalse { ctor.isBroken }
        assertFalse { diag.hasErrors }

        assertNull(ctor.params)
        assertEquals(tokens[0], ctor.thisToken)
        assertFalse { ctor.body.isBroken }
        assertTrue { ctor.body.nodes.isEmpty() }
    }

    @Test
    fun `parse while loop`() {
        val assignmentTokens = listOf(
            Token(TokenKind.VAR, Position(2u, 4u, "test")),
            Token("x", TokenKind.IDENTIFIER, Position(2u, 8u, "test")),
            Token(TokenKind.COLON, Position(2u, 10u, "test")),
            Token("5", TokenKind.INT_LITERAL, Position(2u, 12u, "test")),
        )

        val tokens = listOf(
            Token(TokenKind.WHILE, Position(1u, 1u, "test")),
            Token(TokenKind.TRUE, Position(1u, 6u, "test")),
            Token(TokenKind.LOOP, Position(1u, 12u, "test")),
            *assignmentTokens.toTypedArray(),
            Token(TokenKind.END, Position(3u, 1u, "test")),
        )

        val loop = parser.parseWhileLoop(Tokens(tokens, emptyList()))
        assertFalse { loop.isBroken }
        assertFalse { diag.hasErrors }

        assertEquals(tokens[0], loop.keyword)
        val cond = loop.cond
        assertIs<BooleanLiteral>(cond)
        assertEquals(true, cond.value)

        assertEquals(tokens[2], loop.body.isToken)
        assertEquals(1, loop.body.nodes.size)
        assertIs<VarDecl>(loop.body.nodes.first())

        assertEquals(tokens.last(), loop.body.endToken)
    }

    @Test
    fun `parse if then`() {
        val assignmentTokens = listOf(
            Token(TokenKind.VAR, Position(2u, 4u, "test")),
            Token("x", TokenKind.IDENTIFIER, Position(2u, 8u, "test")),
            Token(TokenKind.COLON, Position(2u, 10u, "test")),
            Token("5", TokenKind.INT_LITERAL, Position(2u, 12u, "test")),
        )

        val tokens = listOf(
            Token(TokenKind.IF, Position(1u, 1u, "test")),
            Token(TokenKind.TRUE, Position(1u, 6u, "test")),
            Token(TokenKind.THEN, Position(1u, 12u, "test")),
            *assignmentTokens.toTypedArray(),
            Token(TokenKind.END, Position(3u, 1u, "test")),
        )

        val ifnode = parser.parseIfNode(Tokens(tokens, emptyList()))
        assertFalse { ifnode.isBroken }
        assertFalse { diag.hasErrors }

        assertEquals(tokens[0], ifnode.ifToken)
        val cond = ifnode.cond
        assertIs<BooleanLiteral>(cond)
        assertEquals(true, cond.value)

        assertEquals(tokens[2], ifnode.thenBody.isToken)
        assertEquals(1, ifnode.thenBody.nodes.size)
        assertIs<VarDecl>(ifnode.thenBody.nodes.first())

        assertEquals(tokens.last(), ifnode.thenBody.endToken)
        assertNull(ifnode.elseBody)
    }

    @Test
    fun `parse if then else`() {
        val assignmentTokens = listOf(
            Token(TokenKind.VAR, Position(2u, 4u, "test")),
            Token("x", TokenKind.IDENTIFIER, Position(2u, 8u, "test")),
            Token(TokenKind.COLON, Position(2u, 10u, "test")),
            Token("5", TokenKind.INT_LITERAL, Position(2u, 12u, "test")),
        )

        val tokens = listOf(
            Token(TokenKind.IF, Position(1u, 1u, "test")),
            Token(TokenKind.TRUE, Position(1u, 6u, "test")),
            Token(TokenKind.THEN, Position(1u, 12u, "test")),
            *assignmentTokens.toTypedArray(),
            Token(TokenKind.ELSE, Position(3u, 1u, "test")),
            *assignmentTokens.toTypedArray(),
            Token(TokenKind.END, Position(5u, 1u, "test")),
        )

        val ifnode = parser.parseIfNode(Tokens(tokens, emptyList()))
        assertFalse { ifnode.isBroken }
        assertFalse { diag.hasErrors }

        assertEquals(tokens[0], ifnode.ifToken)
        val cond = ifnode.cond
        assertIs<BooleanLiteral>(cond)
        assertEquals(true, cond.value)

        assertEquals(tokens[2], ifnode.thenBody.isToken)
        assertEquals(1, ifnode.thenBody.nodes.size)
        assertIs<VarDecl>(ifnode.thenBody.nodes.first())

        assertEquals(tokens[7], ifnode.thenBody.endToken)
        val elseBody = ifnode.elseBody
        assertNotNull(elseBody)
        assertEquals(null, elseBody.isToken)
        assertEquals(1, elseBody.nodes.size)
        assertIs<VarDecl>(elseBody.nodes.first())
        assertEquals(tokens.last(), elseBody.endToken)
    }

    @Test
    fun `parse return`() {
        val tokens = listOf(
            Token(TokenKind.RETURN, Position(1u, 1u, "test")),
        )

        val ret = parser.parseReturnNode(Tokens(tokens, emptyList()))

        assertEquals(tokens[0], ret.keyword)
        assertNull(ret.expr)
    }

    @Test
    fun `parse return value`() {
        val tokens = listOf(
            Token(TokenKind.RETURN, Position(1u, 1u, "test")),
            Token("5", TokenKind.INT_LITERAL, Position(1u, 8u, "test"))
        )

        val ret = parser.parseReturnNode(Tokens(tokens, emptyList()))

        assertEquals(tokens[0], ret.keyword)
        val retVal = ret.expr
        assertNotNull(retVal)
        assertIs<IntegerLiteral>(retVal)
    }

    @Test
    fun `parse assignment`() {
        val tokens = listOf(
            Token("x", TokenKind.IDENTIFIER, Position(1u, 1u, "test")),
            Token(TokenKind.ASSIGN, Position(1u, 3u, "test")),
            Token("5", TokenKind.INT_LITERAL, Position(1u, 6u, "test"))
        )

        val asn = parser.parseAssignment(Tokens(tokens, emptyList()))

        assertEquals(tokens[0], asn.identifierToken)
        assertEquals(tokens[1], asn.assignmentToken)
        val rhs = asn.expr
        assertIs<IntegerLiteral>(rhs)
        assertEquals(tokens.last().value.toLong(), rhs.value)
    }
}