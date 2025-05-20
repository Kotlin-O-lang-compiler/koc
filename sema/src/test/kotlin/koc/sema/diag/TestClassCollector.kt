package koc.sema.diag

import koc.core.Diagnostics
import koc.lex.Lexer
import koc.lex.formatTokens
import koc.lex.fromOptions
import koc.parser.Parser
import koc.parser.fromOptions
import koc.sema.TypeManager
import koc.sema.impl.ClassCollector
import koc.sema.performSemaStage
import koc.sema.semaVisitors
import org.junit.jupiter.api.BeforeEach
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TestClassCollector {
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
    fun `test redefinition`() {
        val code = """
            class A is end
            class B is end
            class C is end
            class B is end
            class D is end
        """.trimIndent()

        val tokens = lexer.lex(code)
        val nodes = parser.parseNodes(tokens)
        assertFalse(diag.hasErrors)

        semaVisitors(typeManager, diag).dropLastWhile { it !is ClassCollector }.forEach { stage ->
            performSemaStage(nodes, stage)
        }

        assertTrue(diag.hasErrors)
    }
}