package koc.sema.diag

import koc.ast.CallExpr
import koc.ast.ClassDecl
import koc.ast.MemberAccessExpr
import koc.ast.MethodRefType
import koc.ast.VarDecl
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
        assertSame(msg.call, call)
        assertTrue(call.ref.type.isMethod)
        assertSame(foo, (call.ref.type as MethodRefType).method)
    }
}