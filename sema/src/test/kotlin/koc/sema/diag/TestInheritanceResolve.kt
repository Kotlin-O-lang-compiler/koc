package koc.sema.diag

import koc.ast.ClassDecl
import koc.core.Diagnostics
import koc.lex.Lexer
import koc.lex.fromOptions
import koc.parser.Parser
import koc.parser.fromOptions
import koc.sema.TypeManager
import koc.sema.impl.ReferenceResolver
import koc.sema.impl.SuperTypeResolver
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

class TestInheritanceResolve {
    private lateinit var diag: Diagnostics
    private lateinit var lexer: Lexer
    private lateinit var parser: Parser
    private lateinit var typeManager: TypeManager

    @BeforeEach
    fun before() {
        diag = Diagnostics()
        lexer = Lexer.fromOptions(diag = diag)
        parser = Parser.fromOptions(diag = diag)
        typeManager = TypeManager(Lexer.fromOptions(diag = Diagnostics()), Parser.fromOptions(diag = Diagnostics()))
    }

    @Test
    fun `test implicit super type`() {
        val code = """
            class A is end
        """.trimIndent()

        val tokens = lexer.lex(code)
        val nodes = parser.parseNodes(tokens)
        assertFalse(diag.hasErrors)

        semaVisitors(typeManager, diag).dropLastWhile { it !is SuperTypeResolver }.forEach { stage ->
            performSemaStage(nodes, stage, typeManager)
        }

        assertFalse(diag.hasErrors)

        val cd = (nodes[0] as ClassDecl)
        val superType = cd.superType
        assertNull(cd.superTypeRef)
        assertNotNull(superType)
        assertSame(typeManager.classType, superType)
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

        semaVisitors(typeManager, diag).dropLastWhile { it !is SuperTypeResolver }.forEach { stage ->
            performSemaStage(nodes, stage, typeManager)
        }

        assertFalse(diag.hasErrors)

        val a = (nodes[0] as ClassDecl)
        val b = (nodes[1] as ClassDecl)
        val superType = a.superType
        assertNotNull(a.superTypeRef)
        assertNotNull(superType)
        assertSame(b.type, superType)
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

        semaVisitors(typeManager, diag).dropLastWhile { it !is SuperTypeResolver }.forEach { stage ->
            performSemaStage(nodes, stage, typeManager)
        }

        assertTrue(diag.hasErrors)
        assertTrue(diag.has<UndefinedReferenceKind>())
        val undefinedRef = (diag.diagnostics.last() as UndefinedReference<*>).ref
        assertEquals(undefinedId, undefinedRef.identifier.value)
        val cd = nodes[0] as ClassDecl

        assertNotNull(cd.superTypeRef)
        assertIs<ClassDecl>(cd.superTypeRef!!.ref)
        assertTrue(with(typeManager) { (cd.superTypeRef!!.ref!! as ClassDecl).isInvalid })
    }
}