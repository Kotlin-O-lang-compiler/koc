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
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame

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
        typeManager = TypeManager(lexer, parser)
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
            performSemaStage(nodes, stage)
        }

        assertFalse(diag.hasErrors)

        val cd = (nodes[0] as ClassDecl)
        val superType = cd.superType
        assertNull(cd.superTypeRef)
        assertNotNull(superType)
        assertSame(typeManager.classType, superType)
    }
}