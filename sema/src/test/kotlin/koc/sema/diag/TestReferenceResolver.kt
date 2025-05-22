package koc.sema.diag

import koc.ast.ClassDecl
import koc.ast.FieldDecl
import koc.ast.MemberAccessExpr
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
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class TestReferenceResolver {
    private lateinit var diag: Diagnostics
    private lateinit var lexer: Lexer
    private lateinit var parser: Parser
    private lateinit var typeManager: TypeManager

    @BeforeEach
    fun before() {
        diag = Diagnostics()
        lexer = Lexer.fromOptions(diag = diag)
        parser = Parser.fromOptions(diag = diag)
        typeManager = TypeManager(lexer, parser)
    }

    @Test
    fun `test undefined this`() {
        val code = """
            this
        """.trimIndent()

        val tokens = lexer.lex(code)
        val nodes = listOf(parser.parseRefExpr(tokens))
        assertFalse(diag.hasErrors)

        semaVisitors(typeManager, diag).dropLastWhile { it !is ReferenceResolver }.forEach { stage ->
            performSemaStage(nodes, stage)
        }

        assertTrue(diag.hasErrors)
        assertTrue(diag.has<ThisOutOfContextKind>())
        val ref = (diag.diagnostics.last() as ThisOutOfContext).ref
        assertNotNull(ref.ref)
        assertIs<ClassDecl>(ref.ref)
        assertTrue(with(typeManager) { (ref.ref!! as ClassDecl).isInvalid })
    }

    @Test
    fun `test undefined super type`() {
        val undefinedId = "AbraCadabra"
        val code = """
            class A extends $undefinedId is end
        """.trimIndent()

        val tokens = lexer.lex(code)
        val nodes = parser.parseNodes(tokens)
        assertFalse(diag.hasErrors)

        semaVisitors(typeManager, diag).dropLastWhile { it !is ReferenceResolver }.forEach { stage ->
            performSemaStage(nodes, stage)
        }

        assertTrue(diag.hasErrors)
        assertTrue(diag.has<UndefinedReferenceKind>())
        val undefinedRef = (diag.diagnostics.last() as UndefinedReference).ref
        assertEquals(undefinedId, undefinedRef.identifier.value)
        val cd = nodes[0] as ClassDecl

        assertNotNull(cd.superTypeRef)
        assertIs<ClassDecl>(cd.superTypeRef!!.ref)
        assertTrue(with(typeManager) { (cd.superTypeRef!!.ref!! as ClassDecl).isInvalid })
    }

    @Test
    fun `test explicit super type`() {
        val code = """
            class A extends B is end
            class B is end
        """.trimIndent()

        val tokens = lexer.lex(code)
        val nodes = parser.parseNodes(tokens)
        assertFalse(diag.hasErrors)

        val a = nodes[0] as ClassDecl
        val b = nodes[1] as ClassDecl

        semaVisitors(typeManager, diag).dropLastWhile { it !is ReferenceResolver }.forEach { stage ->
            performSemaStage(nodes, stage)
        }

        assertFalse(diag.hasErrors)

        assertNull(b.superTypeRef)
        assertNotNull(a.superTypeRef)
        assertNotNull(a.superTypeRef!!.ref)
        assertEquals(b.identifier, a.superTypeRef!!.ref!!.identifier)
        assertFalse(a.superTypeRef!!.isBroken)
    }

    @Test
    fun `test this reference`() {
        val code = """
            class A is
                var x: 0
                var y: this.x
            end
        """.trimIndent()

        val tokens = lexer.lex(code)
        val nodes = parser.parseNodes(tokens)
        assertFalse(diag.hasErrors)

        val a = nodes[0] as ClassDecl
        val y = a.body.members.last() as FieldDecl
        val thisRef = (y.varDecl.initializer as MemberAccessExpr).left

        semaVisitors(typeManager, diag).dropLastWhile { it !is ReferenceResolver }.forEach { stage ->
            performSemaStage(nodes, stage)
        }

        assertFalse(diag.hasErrors)

        assertNotNull(thisRef.ref)
        assertEquals(a.identifier, thisRef.ref!!.identifier)
        assertSame(a, thisRef.ref!!)
    }
}