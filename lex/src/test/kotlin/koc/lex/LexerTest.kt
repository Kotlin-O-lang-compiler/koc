package koc.lex

import koc.utils.Diagnostics
import org.junit.jupiter.api.BeforeEach
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LexerTest {
    private val diag = Diagnostics()
    private val lexer = LexerImpl(diag, false)

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
        assertEquals(1, tokens.size)
        assertEquals(TokenKind.INVALID, tokens.first().kind)
        assertTrue { diag.hasErrors }
    }

    @Test
    fun `test single line comment`() {
        val comment = "//hello world"
        lexer.open(comment)
        val tokens = lexer.lex()
        assertEquals(1, tokens.size)
        assertEquals(TokenKind.COMMENT, tokens.first().kind)
        assertEquals(comment, tokens.first().value)
        assertFalse { diag.hasErrors }
    }

    @Test
    fun `test single line comment lines`() {
        val comment = "//hello world"
        lexer.open("$comment\n\n")
        val tokens = lexer.lex()
        assertEquals(1, tokens.size)
        assertEquals(TokenKind.COMMENT, tokens.first().kind)
        assertEquals(comment, tokens.first().value)
        assertFalse { diag.hasErrors }
    }

    @Test
    fun `test single line comment in comment`() {
        val comment = "// hello world // hello world"
        lexer.open("$comment\n")
        val tokens = lexer.lex()
        assertEquals(1, tokens.size)
        assertEquals(TokenKind.COMMENT, tokens.first().kind)
        assertEquals(comment, tokens.first().value)
        assertFalse { diag.hasErrors }
    }

    @Test
    fun `test single line comment wrapped`() {
        val comment = "// hello world"
        lexer.open("class A is $comment\n\nend")
        val tokens = lexer.lex()
        assertEquals(5, tokens.size)
        assertEquals(TokenKind.CLASS, tokens[0].kind)
        assertEquals(TokenKind.IDENTIFIER, tokens[1].kind)
        assertEquals(TokenKind.IS, tokens[2].kind)
        assertEquals(TokenKind.COMMENT, tokens[3].kind)
        assertEquals(comment, tokens[3].value)
        assertEquals(TokenKind.END, tokens[4].kind)
        assertFalse { diag.hasErrors }

        val dumpStream = ByteArrayOutputStream()
        tokens.dump(PrintStream(dumpStream))
        val dumpLines = dumpStream.toString().lines()

        assertEquals(tokens.size, dumpLines.size)
        assertEquals("Token(CLASS: `class`, program:1:1)", dumpLines[0])
        assertEquals("Token(IDENTIFIER: `A`, program:1:7)", dumpLines[1])
        assertEquals("Token(IS: `is`, program:1:9)", dumpLines[2])
        assertEquals("Token(COMMENT: `// hello world`, program:1:12)", dumpLines[3])
        assertEquals("Token(END: `end`, program:3:1)", dumpLines[4])
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
    fun `test invalid real`() {
        lexer.open("""
            4.21.3
        """.trimIndent())
        lexer.lex()

        assertTrue(diag.hasErrors)
    }

    @Test
    fun `test invalid identifier`() {
        lexer.open("""
            4variable := 6
        """.trimIndent())
        lexer.lex()

        assertTrue(diag.hasErrors)
    }

    @Test
    fun `test multiple arguments method`() {
        lexer.open("""
            method test(a: Integer, b: Integer)
        """.trimIndent())
        val expectedTokens = listOf(
            TokenKind.METHOD, TokenKind.IDENTIFIER, TokenKind.LPAREN, TokenKind.IDENTIFIER, TokenKind.COLON,
            TokenKind.IDENTIFIER, TokenKind.COMMA, TokenKind.IDENTIFIER, TokenKind.COLON, TokenKind.IDENTIFIER,
            TokenKind.RPAREN
        )

        compareTokens(expectedTokens)
    }

    @Test
    fun `test expression method body`() {
        lexer.open("""
            method test => 1
        """.trimIndent())
        val expectedTokens = listOf(
            TokenKind.METHOD, TokenKind.IDENTIFIER, TokenKind.WIDE_ARROW, TokenKind.INT_LITERAL
        )

        compareTokens(expectedTokens)
    }

    @Test
    fun `test assignment`() {
        lexer.open("""
            b := 3
        """.trimIndent())
        val expectedTokens = listOf(TokenKind.IDENTIFIER, TokenKind.ASSIGN, TokenKind.INT_LITERAL)

        compareTokens(expectedTokens)
    }

    @Test
    fun `test variable declaration`() {
        lexer.open("""
            var b : 3
        """.trimIndent())
        val expectedTokens = listOf(TokenKind.VAR, TokenKind.IDENTIFIER, TokenKind.COLON, TokenKind.INT_LITERAL)

        compareTokens(expectedTokens)
    }

    @Test
    fun `test generics`() {
        lexer.open("""
            class Array[T] extends AnyRef
        """.trimIndent())
        val expectedTokens = listOf(
            TokenKind.CLASS, TokenKind.IDENTIFIER, TokenKind.LSQUARE, TokenKind.IDENTIFIER, TokenKind.RSQUARE,
            TokenKind.EXTENDS, TokenKind.IDENTIFIER
        )

        compareTokens(expectedTokens)
    }

    @Test
    fun `test loop`() {
        lexer.open("""
            while i.LessEqual(a) loop
                x := a.get(i)
                a.set(i,x.Mult())
            end
        """.trimIndent())
        val expectedTokens = listOf(
            TokenKind.WHILE, TokenKind.IDENTIFIER, TokenKind.DOT, TokenKind.IDENTIFIER, TokenKind.LPAREN,
            TokenKind.IDENTIFIER, TokenKind.RPAREN, TokenKind.LOOP, TokenKind.IDENTIFIER, TokenKind.ASSIGN,
            TokenKind.IDENTIFIER, TokenKind.DOT,  TokenKind.IDENTIFIER, TokenKind.LPAREN, TokenKind.IDENTIFIER,
            TokenKind.RPAREN, TokenKind.IDENTIFIER, TokenKind.DOT, TokenKind.IDENTIFIER, TokenKind.LPAREN,
            TokenKind.IDENTIFIER, TokenKind.COMMA, TokenKind.IDENTIFIER, TokenKind.DOT, TokenKind.IDENTIFIER,
            TokenKind.LPAREN, TokenKind.RPAREN, TokenKind.RPAREN, TokenKind.END
        )

        compareTokens(expectedTokens)
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
        val expectedTokens = listOf(
            TokenKind.CLASS, TokenKind.IDENTIFIER, TokenKind.IS, TokenKind.METHOD, TokenKind.IDENTIFIER,
            TokenKind.LPAREN, TokenKind.IDENTIFIER, TokenKind.COLON, TokenKind.IDENTIFIER, TokenKind.RPAREN,
            TokenKind.COLON, TokenKind.IDENTIFIER, TokenKind.METHOD, TokenKind.IDENTIFIER, TokenKind.LPAREN,
            TokenKind.IDENTIFIER, TokenKind.COLON, TokenKind.IDENTIFIER, TokenKind.RPAREN, TokenKind.COLON,
            TokenKind.IDENTIFIER, TokenKind.IS, TokenKind.IF, TokenKind.IDENTIFIER, TokenKind.DOT, TokenKind.IDENTIFIER,
            TokenKind.LPAREN, TokenKind.INT_LITERAL, TokenKind.RPAREN, TokenKind.THEN, TokenKind.RETURN,
            TokenKind.INT_LITERAL, TokenKind.END, TokenKind.IF, TokenKind.IDENTIFIER, TokenKind.DOT,
            TokenKind.IDENTIFIER, TokenKind.LPAREN, TokenKind.INT_LITERAL, TokenKind.RPAREN, TokenKind.THEN,
            TokenKind.RETURN, TokenKind.INT_LITERAL, TokenKind.END, TokenKind.RETURN, TokenKind.IDENTIFIER,
            TokenKind.LPAREN, TokenKind.IDENTIFIER, TokenKind.DOT, TokenKind.IDENTIFIER, TokenKind.LPAREN,
            TokenKind.INT_LITERAL, TokenKind.RPAREN, TokenKind.RPAREN, TokenKind.DOT, TokenKind.IDENTIFIER,
            TokenKind.LPAREN, TokenKind.IDENTIFIER, TokenKind.LPAREN, TokenKind.IDENTIFIER, TokenKind.DOT,
            TokenKind.IDENTIFIER, TokenKind.LPAREN, TokenKind.INT_LITERAL, TokenKind.RPAREN, TokenKind.RPAREN,
            TokenKind.RPAREN, TokenKind.END, TokenKind.END,
        )

        compareTokens(expectedTokens)
    }

    private fun compareTokens(expectedTokens: List<TokenKind>) {
        val tokens = lexer.lex()

        assertFalse(diag.hasErrors)
        assertEquals(expectedTokens, tokens.map { it.kind })
    }
}
