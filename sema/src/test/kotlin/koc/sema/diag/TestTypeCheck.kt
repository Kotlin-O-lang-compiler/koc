package koc.sema.diag

import koc.ast.*
import koc.core.Diagnostics
import koc.lex.Lexer
import koc.lex.fromOptions
import koc.parser.Parser
import koc.parser.fromOptions
import koc.sema.TypeManager
import koc.sema.impl.ReferenceResolver
import koc.sema.performSemaStage
import koc.sema.semaVisitors
import org.junit.jupiter.api.BeforeEach
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

class TestTypeCheck {
    private lateinit var diag: Diagnostics
    private lateinit var lexer: Lexer
    private lateinit var parser: Parser
    private lateinit var typeManager: TypeManager

    @BeforeEach
    fun before() {
        diag = Diagnostics()
        lexer = Lexer.fromOptions(diag = diag)
        parser = Parser.fromOptions(diag = diag)
        typeManager = TypeManager(Lexer.fromOptions(diag = diag), Parser.fromOptions(diag = diag))
    }

    @Test
    fun `test void call as expr`() {
        val code = """
            class A is
                this is
                    var x: this.foo()
                end
                method foo is end
            end
        """.trimIndent()

        val tokens = lexer.lex(code)
        val nodes = parser.parseNodes(tokens)
        assertFalse(diag.hasErrors)

        semaVisitors(typeManager, diag).dropLastWhile { it !is ReferenceResolver }.forEach { stage ->
            performSemaStage(nodes, stage, typeManager)
        }

        val a = nodes[0] as ClassDecl
        val ctor = a.constructors.first()
        val x = ctor.body.nodes.first() as VarDecl
        val initializer = x.initializer as MemberAccessExpr
        val foo = a.methods.first()

        assertTrue(diag.hasErrors)
        diag.has<NonReturningCallInExprKind>()
        val msg = diag.diagnostics.first() as NonReturningCallInExpr
        val call = initializer.member as CallExpr
        assertSame(msg.expr, initializer)
        assertSame(typeManager.unitType, call.type)
        assertTrue(call.ref.type.isMethod)
        assertSame(foo, (call.ref.type as MethodRefType).method)
    }

    @Test
    fun `test void call as expr in member access`() {
        val code = """
            class A is
                this is
                    this.foo().foo()
                end
                method foo is end
            end
        """.trimIndent()

        val tokens = lexer.lex(code)
        val nodes = parser.parseNodes(tokens)
        assertFalse(diag.hasErrors)

        semaVisitors(typeManager, diag).dropLastWhile { it !is ReferenceResolver }.forEach { stage ->
            performSemaStage(nodes, stage, typeManager)
        }

        val a = nodes[0] as ClassDecl
        val ctor = a.constructors.first()
        val callChain = ctor.body.nodes.first() as MemberAccessExpr
        val thisExpr = callChain.left as RefExpr
        val foo = a.methods.first()
        val fooAccess = callChain.member as MemberAccessExpr
        val foo1 = fooAccess.left as CallExpr
        val foo2 = fooAccess.member as CallExpr

        assertTrue(diag.hasErrors)
        diag.has<NonReturningCallInExprKind>()
        val msg = diag.diagnostics.first() as NonReturningCallInExpr
        assertSame(msg.expr, foo1)
        assertSame(typeManager.unitType, foo1.type)
        assertTrue(foo1.ref.type.isMethod)
        assertSame(foo, (foo1.ref.type as MethodRefType).method)
    }

    @Test
    fun `test void call as statement`() {
        val code = """
            class A is
                this is
                    this.foo()
                end
                method foo is end
            end
        """.trimIndent()

        val tokens = lexer.lex(code)
        val nodes = parser.parseNodes(tokens)
        assertFalse(diag.hasErrors)

        semaVisitors(typeManager, diag).dropLastWhile { it !is ReferenceResolver }.forEach { stage ->
            performSemaStage(nodes, stage, typeManager)
        }

        assertFalse(diag.hasErrors)
    }
}