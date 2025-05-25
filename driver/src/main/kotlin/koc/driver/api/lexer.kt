package koc.driver.api

import koc.core.Diagnostics
import koc.core.KocOptions
import koc.lex.Lexer
import koc.lex.LexerImpl
import koc.lex.Tokens
import java.nio.file.Path

fun lex(
    source: Path,
    diag: Diagnostics = Diagnostics(),
    options: KocOptions,
): Tokens = lex(diag, options) { open(source.toFile()) }

fun lex(
    source: String,
    sourceName: String = "program",
    diag: Diagnostics = Diagnostics(),
    options: KocOptions,
): Tokens = lex(diag, options) { open(source, sourceName) }

private fun lex(
    diag: Diagnostics,
    options: KocOptions,
    openSource: Lexer.() -> Unit,
): Tokens {
    val lexer = LexerImpl(diag, options.stopOnError).apply { openSource() }

    return lexer.use { lexer.lex() }
}
