package koc.driver.api

import java.nio.file.Path
import koc.driver.KocOptions
import koc.lex.Lexer
import koc.lex.LexerImpl
import koc.lex.Token
import koc.utils.Diagnostics

fun lex(
    source: Path,
    diag: Diagnostics = Diagnostics(),
    options: KocOptions,
): List<Token> = lex(diag, options) { open(source.toFile()) }

fun lex(
    source: String,
    sourceName: String = "program",
    diag: Diagnostics = Diagnostics(),
    options: KocOptions,
): List<Token> = lex(diag, options) { open(source, sourceName) }

private fun lex(
    diag: Diagnostics,
    options: KocOptions,
    openSource: Lexer.() -> Unit,
): List<Token> {
    val lexer = LexerImpl(diag, options.stopOnError).apply { openSource() }

    return lexer.use { lexer.lex() }
}
