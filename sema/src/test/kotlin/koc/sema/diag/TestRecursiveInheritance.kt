package koc.sema.diag

import koc.ast.ClassDecl
import koc.core.Diagnostics
import koc.lex.Lexer
import koc.lex.fromOptions
import koc.parser.Parser
import koc.parser.fromOptions
import koc.sema.TypeManager
import koc.sema.impl.SuperTypeResolver
import koc.sema.performSemaStage
import koc.sema.semaVisitors
import org.junit.jupiter.api.BeforeEach
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TestRecursiveInheritance {
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
    fun `test self inheritance`() {
        val code = """
            class A extends A is end
        """.trimIndent()

        val tokens = lexer.lex(code)
        val nodes = parser.parseNodes(tokens)
        assertFalse(diag.hasErrors)
        val a = nodes.first() as ClassDecl

        semaVisitors(typeManager, diag).dropLastWhile { it !is SuperTypeResolver }.forEach { stage ->
            performSemaStage(nodes, stage)
        }

        assertTrue(diag.hasErrors)
        assertTrue(diag.has<RecursiveInheritanceKind>())
        val recursiveChain = (diag.diagnostics.last() as RecursiveInheritance).chain
        assertContains(recursiveChain, a.identifier)
        assertTrue { recursiveChain.none { name -> name != a.identifier } }
    }

    @Test
    fun `test direct recursion`() {
        val code = """
            class A extends B is end
            class B extends A is end
        """.trimIndent()

        val tokens = lexer.lex(code)
        val nodes = parser.parseNodes(tokens)
        assertFalse(diag.hasErrors)

        semaVisitors(typeManager, diag).dropLastWhile { it !is SuperTypeResolver }.forEach { stage ->
            performSemaStage(nodes, stage)
        }

        assertTrue(diag.hasErrors)
        assertTrue(diag.has<RecursiveInheritanceKind>())
        val recursiveChain = (diag.diagnostics.last() as RecursiveInheritance).chain
        assertContains(recursiveChain, (nodes[0] as ClassDecl).identifier)
        assertContains(recursiveChain, (nodes[1] as ClassDecl).identifier)
    }

    @Test
    fun `test indirect recursion`() {
        val code = """
            class A extends B is end
            class B extends C is end
            class C extends D is end
            class D extends E is end
            class E extends C is end
        """.trimIndent()

        val tokens = lexer.lex(code)
        val nodes = parser.parseNodes(tokens)
        assertFalse(diag.hasErrors)

        semaVisitors(typeManager, diag).dropLastWhile { it !is SuperTypeResolver }.forEach { stage ->
            performSemaStage(nodes, stage)
        }

        assertTrue(diag.hasErrors)
        assertTrue(diag.has<RecursiveInheritanceKind>())
        val recursiveChain = (diag.diagnostics.last() as RecursiveInheritance).chain
        assertContains(recursiveChain, (nodes[0] as ClassDecl).identifier)
        assertContains(recursiveChain, (nodes[1] as ClassDecl).identifier)
        assertContains(recursiveChain, (nodes[2] as ClassDecl).identifier)
        assertContains(recursiveChain, (nodes[3] as ClassDecl).identifier)
        assertContains(recursiveChain, (nodes[4] as ClassDecl).identifier)
    }
}