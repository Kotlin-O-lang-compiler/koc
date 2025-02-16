package koc.lex

import koc.utils.Diagnostics
import org.junit.jupiter.api.BeforeEach
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LexerTest {
    val diag = Diagnostics()
    val lexer = LexerImpl(diag, false)

    @BeforeEach
    fun initialize() {
        diag.clear()
        lexer.close()
    }

    @Test
    fun `test empty program`() {
        lexer.open("")
        val tokens = lexer.lex()
        assertEquals(0, tokens.size)
        assertFalse { diag.hasErrors }
    }

    @Test
    fun `test unexpected program`() {
        lexer.open("+one")
        val tokens = lexer.lex()
        assertEquals(2, tokens.size)
        assertEquals(TokenKind.INVALID, tokens.first().kind)
        assertTrue { diag.hasErrors }
    }

    @Test
    fun `test class dummy`() {
        lexer.open("class A is end")
        val tokens = lexer.lex()
        assertEquals(4, tokens.size)
        assertEquals(TokenKind.CLASS, tokens[0].kind)
        assertEquals(TokenKind.IDENTIFIER, tokens[1].kind)
        assertEquals("A", tokens[1].value)
        assertEquals(TokenKind.IS, tokens[2].kind)
        assertEquals(TokenKind.END, tokens[3].kind)
    }

    @Test
    fun `test class extends`() {
        lexer.open("""
            class A extends B is end
        """.trimIndent())
        val tokens = lexer.lex()
        assertEquals(6, tokens.size)
        assertEquals(TokenKind.CLASS, tokens[0].kind)
        assertEquals(TokenKind.IDENTIFIER, tokens[1].kind)
        assertEquals("A", tokens[1].value)
        assertEquals(TokenKind.EXTENDS, tokens[2].kind)
        assertEquals(TokenKind.IDENTIFIER, tokens[3].kind)
        assertEquals("B", tokens[3].value)
        assertEquals(TokenKind.IS, tokens[4].kind)
        assertEquals(TokenKind.END, tokens[5].kind)
    }


    @Test
    fun `test fibonacci`() {
        lexer.open("""
            class Fibonaccer is
            	method fibonacci(n: Integer): Integer
            	method fibonacci(n: Integer): Integer is
            		if n.Equal(0) then return 0 end
            		if n.Equal(1) then return 1 end
            		return fibonacci(n.Minus(1)).Plus(fibonacci(n.Minus(2)))
            	end
            end
        """.trimIndent())
        val tokens = lexer.lex()
        assertFalse { diag.hasErrors }
    }
}
