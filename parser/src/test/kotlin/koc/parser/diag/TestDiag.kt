package koc.parser.diag

import koc.core.Diagnostics
import koc.core.Position
import koc.lex.Token
import koc.lex.TokenKind
import koc.lex.formatTokens
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.nio.charset.StandardCharsets
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TestDiag {
    private fun <T> withStreams(
        action: (out: PrintStream, err: PrintStream) -> T,
        check: (out: String, err: String, T) -> Unit
    ) {
        val outBytes = ByteArrayOutputStream()
        val charset = StandardCharsets.UTF_8
        val outStream = PrintStream(outBytes, true, charset)
        val errBytes = ByteArrayOutputStream()
        val errStream = PrintStream(errBytes, true, charset)

        outStream.use { out ->
            errStream.use { err ->
                val res = action(out, err)
                check(String(outBytes.toByteArray(), charset), String(errBytes.toByteArray(), charset), res)
            }
        }
    }

    fun assertHasLines(str: String, vararg lines: String, withOrder: Boolean = true) {
        if (lines.isEmpty()) return
        val strLines = str.lines()
        var idx = strLines.indexOf(lines.first())
        assertTrue("Line \"${lines.first()}\" is not met in the string \"$str\"") { idx >= 0 }
        for (line in lines) {
            if (withOrder) {
                assertEquals(line, strLines[idx], "Expected \"${line}\" at index $idx, but was \"${strLines[idx]}\"  ")
                idx++
            } else {
                assertContains(strLines, line)
            }
        }
    }

    @Test
    fun testDiagTokenUnderlineToken() {
        val token = Token(TokenKind.CLASS, Position(1u, 1u, "file"))
        val expected = Token(TokenKind.LPAREN, Position.fake)

        withStreams(
            { out, err ->
                val diag = Diagnostics(outstream = out, errstream = err)
                diag.diag(
                    UnexpectedToken(
                        token,
                        expected.kind,
                        formatTokens(listOf(token), start = 0, end = 0, showLines = false, onlyWindow = true).lines()
                    ), token.start, token.end
                )
                diag
            },
            check = { out, err, diag ->
                assertHasLines(
                    err,
                    "error: Unexpected token 'class', probably you mean '('",
                    " -> file:1:1",
                    "1 | class",
                    "    ~~~~~"
                )
            }
        )
    }

    @Test
    fun testDiagTokenUnderlineTokens() {
        val bad = Token(TokenKind.CLASS, Position(1u, 8u, "file"))
        val tokens = listOf(
            Token(TokenKind.CLASS, Position(1u, 1u, "file")),
            bad,
            Token("MyClass", TokenKind.IDENTIFIER, Position(1u, 14u, "file"))
        )

        val expected = Token(TokenKind.IDENTIFIER, Position.fake)

        withStreams(
            { out, err ->
                val diag = Diagnostics(outstream = out, errstream = err)
                diag.diag(
                    UnexpectedToken(
                        bad,
                        expected.kind,
                        formatTokens(
                            tokens,
                            start = 0,
                            end = tokens.lastIndex,
                            showLines = false,
                            onlyWindow = true
                        ).lines()
                    ), bad.start, bad.end
                )
                diag
            },
            check = { out, err, diag ->
                assertHasLines(
                    err,
                    "error: Unexpected token 'class', probably you mean identifier",
                    " -> file:1:8",
                    "1 | class  class MyClass",
                    "           ~~~~~"
                )
            }
        )
    }

    @Test
    fun testDiagTokenUnderlineTokensMultiline() {
        val bad = Token(TokenKind.CLASS, Position(3u, 4u, "file"))
        val tokens = listOf(
            Token(TokenKind.CLASS, Position(1u, 1u, "file")),
            Token("MyClass", TokenKind.IDENTIFIER, Position(1u, 14u, "file")),
            Token(TokenKind.IS, Position(2u, 4u, "file")),
            bad,
            Token(TokenKind.END, Position(4u, 1u, "file"))
        )

        val expected = Token(TokenKind.IDENTIFIER, Position.fake)

        withStreams(
            { out, err ->
                val diag = Diagnostics(outstream = out, errstream = err)
                diag.diag(
                    UnexpectedToken(
                        bad,
                        expected.kind,
                        formatTokens(
                            tokens,
                            start = 0,
                            end = tokens.lastIndex,
                            showLines = false,
                            onlyWindow = true
                        ).lines()
                    ), bad.start, bad.end
                )
                diag
            },
            check = { out, err, diag ->
                println(err)
                assertHasLines(
                    err,
                    "error: Unexpected token 'class', probably you mean identifier",
                    " -> file:3:4",
                    "2 |    is",
                    "3 |    class",
                    "       ~~~~~"
                )
            }
        )
    }
}