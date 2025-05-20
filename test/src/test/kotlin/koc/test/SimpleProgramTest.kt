package koc.test

import koc.driver.Kocpiler
import koc.ast.ClassDecl
import koc.core.KocOptions
import org.junit.jupiter.api.Assertions.assertEquals
import kotlin.io.path.createTempFile
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertIs

class SimpleProgramTest {
    @Test
    fun `test parse`() {
        val compiler = Kocpiler()
        val tokens = compiler.lexer.lex(code)
        val nodes = compiler.parser.parseNodes(tokens)
        assertEquals(1, nodes.size)
        val classDecl = nodes.first()
        assertIs<ClassDecl>(classDecl)
        assertEquals(2, classDecl.body.members.size)
    }

    @Test
    fun `test all`() {
        val code = createTempFile("testprogram", ".ol")
        code.toFile().deleteOnExit()
        code.writeText(Companion.code)

        val compiler = Kocpiler(opts = KocOptions(dumpTokens = true))
        compiler.run(code)
        assertEquals(false, compiler.diag.hasErrors)
    }

    companion object {
        val code = """
    class Adder is
        var sum: 0
        
        method add(value: Integer) : Integer is
            sum := sum.Plus(value)
            return sum
        end
    end
        """.trimIndent()
    }
}
