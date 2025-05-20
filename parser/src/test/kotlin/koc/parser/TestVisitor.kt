package koc.parser

import koc.lex.Token
import koc.lex.TokenKind
import koc.ast.Body
import koc.ast.ClassBody
import koc.ast.ClassDecl
import koc.ast.FieldDecl
import koc.ast.IntegerLiteral
import koc.ast.MethodBody
import koc.ast.MethodDecl
import koc.ast.Node
import koc.ast.Param
import koc.ast.Params
import koc.ast.VarDecl
import koc.ast.visitor.AbstractVoidVisitor
import koc.ast.visitor.Insight
import koc.parser.impl.ParserImpl
import koc.core.Diagnostics
import koc.core.Position
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TestVisitor {
    private val methodTokens = listOf(
        Token(TokenKind.METHOD, Position(2u, 1u, "test")),
        Token("foo", TokenKind.IDENTIFIER, Position(2u, 8u, "test")),
        Token(TokenKind.IS, Position(2u, 12u, "test")),
        Token(TokenKind.END, Position(2u, 15u, "test")),
    )

    private val varTokens = listOf(
        Token(TokenKind.VAR, Position(3u, 4u, "test")),
        Token("field", TokenKind.IDENTIFIER, Position(3u, 8u, "test")),
        Token(TokenKind.COLON, Position(3u, 14u, "test")),
        Token("0", TokenKind.INT_LITERAL, Position(3u, 16u, "test"))
    )

    private val classTokens = listOf(
        Token(TokenKind.CLASS, Position(1u, 1u, "test")),
        Token("k", TokenKind.IDENTIFIER, Position(1u, 7u, "test")),
        Token(TokenKind.IS, Position(1u, 9u, "test")),
        *methodTokens.toTypedArray(),
        *varTokens.toTypedArray(),
        Token(TokenKind.END, Position(4u, 13u, "test"))
    )

    private val classDecl = ParserImpl(Diagnostics()).parseClassDecl(classTokens)

    @Test
    fun `test top down`() {
        var classVisited = 0
        var classBodyVisited = 0
        var methodVisited = 0
        var fieldVisited = 0
        var varVisited = 0
        var varInitializerVisited = 0
        var visited = 0

        val visitor = object : AbstractVoidVisitor() {
            override fun visit(node: Node) {
                assertTrue(node.inVisit)
                super.visit(node)
                visited++
            }
            override fun previsit(classDecl: ClassDecl) {
                assertTrue(classDecl.inVisit)
                assertEquals(0, classVisited)
                classVisited++
                assertEquals(0, classBodyVisited)
                assertEquals(0, methodVisited)
                assertEquals(0, fieldVisited)
                assertEquals(0, varVisited)
                assertEquals(0, varInitializerVisited)
            }
            override fun visit(classDecl: ClassDecl) {
                assertTrue(classDecl.inVisit)
                assertEquals(1, classVisited)
                classVisited++
                assertEquals(0, classBodyVisited)
                assertEquals(0, methodVisited)
                assertEquals(0, fieldVisited)
                assertEquals(0, varVisited)
                assertEquals(0, varInitializerVisited)
            }

            override fun postvisit(classDecl: ClassDecl, res: Unit) {
                assertTrue(classDecl.inVisit)
                assertEquals(2, classVisited)
                classVisited++
                assertEquals(3, classBodyVisited)
                assertEquals(9, methodVisited)
                assertEquals(3, fieldVisited)
                assertEquals(3, varVisited)
                assertEquals(3, varInitializerVisited)
            }

            override fun previsit(body: ClassBody) {
                assertEquals(2, classVisited)
                assertEquals(0, classBodyVisited)
                classBodyVisited++
                assertEquals(0, methodVisited)
                assertEquals(0, fieldVisited)
                assertEquals(0, varVisited)
                assertEquals(0, varInitializerVisited)
            }
            override fun visit(body: ClassBody) {
                assertEquals(2, classVisited)
                assertEquals(1, classBodyVisited)
                classBodyVisited++
                assertEquals(0, methodVisited)
                assertEquals(0, fieldVisited)
                assertEquals(0, varVisited)
                assertEquals(0, varInitializerVisited)
            }
            override fun postvisit(body: ClassBody, res: Unit) {
                assertEquals(2, classVisited)
                assertEquals(2, classBodyVisited)
                classBodyVisited++
                assertEquals(9, methodVisited)
                assertEquals(3, fieldVisited)
                assertEquals(3, varVisited)
                assertEquals(3, varInitializerVisited)
            }

            override fun previsit(field: FieldDecl) {
                assertEquals(2, classVisited)
                assertEquals(2, classBodyVisited)
                assertTrue { methodVisited == 0 || methodVisited == 9 }
                assertEquals(0, fieldVisited)
                fieldVisited++
                assertEquals(0, varVisited)
                assertEquals(0, varInitializerVisited)
            }
            override fun visit(field: FieldDecl) {
                assertEquals(2, classVisited)
                assertEquals(2, classBodyVisited)
                assertTrue { methodVisited == 0 || methodVisited == 9 }
                assertEquals(1, fieldVisited)
                fieldVisited++
                assertEquals(0, varVisited)
                assertEquals(0, varInitializerVisited)
            }
            override fun postvisit(field: FieldDecl, res: Unit) {
                assertEquals(2, classVisited)
                assertEquals(2, classBodyVisited)
                assertTrue { methodVisited == 0 || methodVisited == 9 }
                assertEquals(2, fieldVisited)
                fieldVisited++
                assertEquals(3, varVisited)
                assertEquals(3, varInitializerVisited)
            }

            override fun previsit(vardecl: VarDecl) {
                assertEquals(2, classVisited)
                assertEquals(2, classBodyVisited)
                assertTrue { methodVisited == 0 || methodVisited == 9 }
                assertEquals(2, fieldVisited)
                assertEquals(0, varVisited)
                varVisited++
                assertEquals(0, varInitializerVisited)
            }
            override fun visit(vardecl: VarDecl) {
                assertEquals(2, classVisited)
                assertEquals(2, classBodyVisited)
                assertTrue { methodVisited == 0 || methodVisited == 9 }
                assertEquals(2, fieldVisited)
                assertEquals(1, varVisited)
                varVisited++
                assertEquals(0, varInitializerVisited)
            }
            override fun postvisit(vardecl: VarDecl, res: Unit) {
                assertEquals(2, classVisited)
                assertEquals(2, classBodyVisited)
                assertTrue { methodVisited == 0 || methodVisited == 9 }
                assertEquals(2, fieldVisited)
                assertEquals(2, varVisited)
                varVisited++
                assertEquals(3, varInitializerVisited)
            }

            override fun previsit(lit: IntegerLiteral) {
                assertEquals(2, classVisited)
                assertEquals(2, classBodyVisited)
                assertTrue { methodVisited == 0 || methodVisited == 9 }
                assertEquals(2, fieldVisited)
                assertEquals(2, varVisited)
                assertEquals(0, varInitializerVisited)
                varInitializerVisited++
            }
            override fun visit(lit: IntegerLiteral) {
                assertEquals(2, classVisited)
                assertEquals(2, classBodyVisited)
                assertTrue { methodVisited == 0 || methodVisited == 9 }
                assertEquals(2, fieldVisited)
                assertEquals(2, varVisited)
                assertEquals(1, varInitializerVisited)
                varInitializerVisited++
            }
            override fun postvisit(lit: IntegerLiteral, res: Unit) {
                assertEquals(2, classVisited)
                assertEquals(2, classBodyVisited)
                assertTrue { methodVisited == 0 || methodVisited == 9 }
                assertEquals(2, fieldVisited)
                assertEquals(2, varVisited)
                assertEquals(2, varInitializerVisited)
                varInitializerVisited++
            }

            override fun previsit(method: MethodDecl) {
                assertEquals(2, classVisited)
                assertEquals(2, classBodyVisited)
                assertEquals(0, methodVisited)
                methodVisited++
                assertTrue { fieldVisited == 0 || fieldVisited == 3 }
                assertTrue { varVisited == 0 || varVisited == 3 }
                assertTrue { varInitializerVisited == 0 || varInitializerVisited == 3 }
            }
            override fun visit(method: MethodDecl) {
                assertEquals(2, classVisited)
                assertEquals(2, classBodyVisited)
                assertEquals(1, methodVisited)
                methodVisited++
                assertTrue { fieldVisited == 0 || fieldVisited == 3 }
                assertTrue { varVisited == 0 || varVisited == 3 }
                assertTrue { varInitializerVisited == 0 || varInitializerVisited == 3 }
            }
            override fun postvisit(method: MethodDecl, res: Unit) {
                assertEquals(2, classVisited)
                assertEquals(2, classBodyVisited)
                assertEquals(8, methodVisited)
                methodVisited++
                assertTrue { fieldVisited == 0 || fieldVisited == 3 }
                assertTrue { varVisited == 0 || varVisited == 3 }
                assertTrue { varInitializerVisited == 0 || varInitializerVisited == 3 }
            }

            override fun previsit(node: MethodBody) {
                assertEquals(2, classVisited)
                assertEquals(2, classBodyVisited)
                assertEquals(2, methodVisited)
                methodVisited++
                assertTrue { fieldVisited == 0 || fieldVisited == 3 }
                assertTrue { varVisited == 0 || varVisited == 3 }
                assertTrue { varInitializerVisited == 0 || varInitializerVisited == 3 }
            }
            override fun visit(node: MethodBody) {
                assertEquals(2, classVisited)
                assertEquals(2, classBodyVisited)
                assertEquals(3, methodVisited)
                methodVisited++
                assertTrue { fieldVisited == 0 || fieldVisited == 3 }
                assertTrue { varVisited == 0 || varVisited == 3 }
                assertTrue { varInitializerVisited == 0 || varInitializerVisited == 3 }
            }
            override fun postvisit(node: MethodBody, res: Unit) {
                assertEquals(2, classVisited)
                assertEquals(2, classBodyVisited)
                assertEquals(7, methodVisited)
                methodVisited++
                assertTrue { fieldVisited == 0 || fieldVisited == 3 }
                assertTrue { varVisited == 0 || varVisited == 3 }
                assertTrue { varInitializerVisited == 0 || varInitializerVisited == 3 }
            }

            override fun previsit(node: Body) {
                assertEquals(2, classVisited)
                assertEquals(2, classBodyVisited)
                assertEquals(4, methodVisited)
                methodVisited++
                assertTrue { fieldVisited == 0 || fieldVisited == 3 }
                assertTrue { varVisited == 0 || varVisited == 3 }
                assertTrue { varInitializerVisited == 0 || varInitializerVisited == 3 }
            }
            override fun visit(node: Body) {
                assertEquals(2, classVisited)
                assertEquals(2, classBodyVisited)
                assertEquals(5, methodVisited)
                methodVisited++
                assertTrue { fieldVisited == 0 || fieldVisited == 3 }
                assertTrue { varVisited == 0 || varVisited == 3 }
                assertTrue { varInitializerVisited == 0 || varInitializerVisited == 3 }
            }
            override fun postvisit(node: Body, res: Unit) {
                assertEquals(2, classVisited)
                assertEquals(2, classBodyVisited)
                assertEquals(6, methodVisited)
                methodVisited++
                assertTrue { fieldVisited == 0 || fieldVisited == 3 }
                assertTrue { varVisited == 0 || varVisited == 3 }
                assertTrue { varInitializerVisited == 0 || varInitializerVisited == 3 }
            }

            override fun visit(node: Params) {
                assertFalse(true)
            }
            override fun visit(node: Param) {
                assertFalse(true)
            }
        }

        classDecl.visit(visitor)
        assertEquals(3, classVisited)
        assertEquals(3, classBodyVisited)
        assertEquals(9, methodVisited)
        assertEquals(3, fieldVisited)
        assertEquals(3, varVisited)
        assertEquals(3, varInitializerVisited)

        // ClassDecl -> ClassBody -> MethodDecl -> MBody -> Body -> FieldDecl -> VarDecl -> IntegerLiteral
        assertEquals(8, visited)
    }

    @ParameterizedTest
    @EnumSource(value = Insight::class, names = ["STOP", "SKIP"])
    fun `test top down stop`(stop: Insight) {
        var classVisited = 0
        var classBodyVisited = 0
        var visited = 0

        val visitor = object : AbstractVoidVisitor() {
            override fun visit(node: Node) {
                super.visit(node)
                visited++
            }
            override fun previsit(classDecl: ClassDecl) {
                assertEquals(0, classVisited)
                classVisited++
                assertEquals(0, classBodyVisited)
            }
            override fun visit(classDecl: ClassDecl) {
                assertEquals(1, classVisited)
                classVisited++
                assertEquals(0, classBodyVisited)
            }

            override fun postvisit(classDecl: ClassDecl, res: Unit) {
                assertEquals(2, classVisited)
                classVisited++
                assertEquals(3, classBodyVisited)
            }

            override fun previsit(body: ClassBody) {
                assertEquals(2, classVisited)
                assertEquals(0, classBodyVisited)
                classBodyVisited++
            }
            override fun visit(body: ClassBody) {
                assertEquals(2, classVisited)
                assertEquals(1, classBodyVisited)
                classBodyVisited++
                this.emit(stop)
            }
            override fun postvisit(body: ClassBody, res: Unit) {
                assertEquals(2, classVisited)
                assertEquals(2, classBodyVisited)
                classBodyVisited++
            }

            override fun previsit(field: FieldDecl) {
                assertFalse(true)
            }
            override fun visit(field: FieldDecl) {
                assertFalse(true)
            }
            override fun postvisit(field: FieldDecl, res: Unit) {
                assertFalse(true)
            }

            override fun previsit(vardecl: VarDecl) {
                assertFalse(true)
            }
            override fun visit(vardecl: VarDecl) {
                assertFalse(true)
            }
            override fun postvisit(vardecl: VarDecl, res: Unit) {
                assertFalse(true)
            }

            override fun previsit(lit: IntegerLiteral) {
                assertFalse(true)
            }
            override fun visit(lit: IntegerLiteral) {
                assertFalse(true)
            }
            override fun postvisit(lit: IntegerLiteral, res: Unit) {
                assertFalse(true)
            }

            override fun previsit(method: MethodDecl) {
                assertFalse(true)
            }
            override fun visit(method: MethodDecl) {
                assertFalse(true)
            }
            override fun postvisit(method: MethodDecl, res: Unit) {
                assertFalse(true)
            }

            override fun previsit(node: MethodBody) {
                assertFalse(true)
            }
            override fun visit(node: MethodBody) {
                assertFalse(true)
            }
            override fun postvisit(node: MethodBody, res: Unit) {
                assertFalse(true)
            }

            override fun previsit(node: Body) {
                assertFalse(true)
            }
            override fun visit(node: Body) {
                assertFalse(true)
            }
            override fun postvisit(node: Body, res: Unit) {
                assertFalse(true)
            }

            override fun visit(node: Params) {
                assertFalse(true)
            }
            override fun visit(node: Param) {
                assertFalse(true)
            }
        }

        classDecl.visit(visitor)
        assertEquals(3, classVisited)
        assertEquals(3, classBodyVisited)

        // ClassDecl -> ClassBody
        assertEquals(2, visited)
    }

    @Test
    fun `test top down skip`() {
        var classVisited = 0
        var classBodyVisited = 0
        var fieldVisited = 0
        var methodVisited = 0
        var visited = 0

        val visitor = object : AbstractVoidVisitor() {
            override fun visit(node: Node) {
                super.visit(node)
                visited++
            }
            override fun previsit(classDecl: ClassDecl) {
                assertEquals(0, classVisited)
                classVisited++
                assertEquals(0, classBodyVisited)
            }
            override fun visit(classDecl: ClassDecl) {
                assertEquals(1, classVisited)
                classVisited++
                assertEquals(0, classBodyVisited)
            }

            override fun postvisit(classDecl: ClassDecl, res: Unit) {
                assertEquals(2, classVisited)
                classVisited++
                assertEquals(3, classBodyVisited)
            }

            override fun previsit(body: ClassBody) {
                assertEquals(2, classVisited)
                assertEquals(0, classBodyVisited)
                classBodyVisited++
            }
            override fun visit(body: ClassBody) {
                assertEquals(2, classVisited)
                assertEquals(1, classBodyVisited)
                classBodyVisited++
            }
            override fun postvisit(body: ClassBody, res: Unit) {
                assertEquals(2, classVisited)
                assertEquals(2, classBodyVisited)
                classBodyVisited++
            }

            override fun previsit(field: FieldDecl) {
                assertEquals(0, fieldVisited)
                fieldVisited++
            }
            override fun visit(field: FieldDecl) {
                assertEquals(1, fieldVisited)
                fieldVisited++
                emit(Insight.SKIP)
            }
            override fun postvisit(field: FieldDecl, res: Unit) {
                assertEquals(2, fieldVisited)
                fieldVisited++
            }

            override fun previsit(vardecl: VarDecl) {
                assertFalse(true)
            }
            override fun visit(vardecl: VarDecl) {
                assertFalse(true)
            }
            override fun postvisit(vardecl: VarDecl, res: Unit) {
                assertFalse(true)
            }

            override fun previsit(lit: IntegerLiteral) {
                assertFalse(true)
            }
            override fun visit(lit: IntegerLiteral) {
                assertFalse(true)
            }
            override fun postvisit(lit: IntegerLiteral, res: Unit) {
                assertFalse(true)
            }

            override fun previsit(method: MethodDecl) {
                assertEquals(0, methodVisited)
                methodVisited++
            }
            override fun visit(method: MethodDecl) {
                assertEquals(1, methodVisited)
                methodVisited++
                emit(Insight.SKIP)
            }
            override fun postvisit(method: MethodDecl, res: Unit) {
                assertEquals(2, methodVisited)
                methodVisited++
            }

            override fun previsit(node: MethodBody) {
                assertFalse(true)
            }
            override fun visit(node: MethodBody) {
                assertFalse(true)
            }
            override fun postvisit(node: MethodBody, res: Unit) {
                assertFalse(true)
            }

            override fun previsit(node: Body) {
                assertFalse(true)
            }
            override fun visit(node: Body) {
                assertFalse(true)
            }
            override fun postvisit(node: Body, res: Unit) {
                assertFalse(true)
            }

            override fun visit(node: Params) {
                assertFalse(true)
            }
            override fun visit(node: Param) {
                assertFalse(true)
            }
        }

        classDecl.visit(visitor)
        assertEquals(3, classVisited)
        assertEquals(3, classBodyVisited)
        assertEquals(3, fieldVisited)
        assertEquals(3, methodVisited)

        // ClassDecl -> ClassBody -> FieldDecl -> MethodDecl
        assertEquals(4, visited)
    }

    @Test
    fun `test bottom up`() {

    }
}