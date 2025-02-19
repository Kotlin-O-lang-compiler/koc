package koc.driver.api

import koc.lex.dump
import koc.utils.Diagnostics
import java.nio.file.Path
import koc.driver.KocOptions
import koc.lex.Token
import kotlin.io.path.name

fun koc(
    programs: List<Path>,
    diag: Diagnostics = Diagnostics(),
    options: KocOptions,
) {
    for (program in programs) {
        val tokens = lex(program, diag, options)

        dumpTokens(tokens, program.name, options)

        // TODO("Parser is not implemented yet")
    }
}

fun kocFromCode(
    programs: List<String>,
    diag: Diagnostics = Diagnostics(),
    options: KocOptions,
) {
    programs.forEachIndexed { idx, program ->
        val programName = "program-$idx"
        val tokens = lex(program, programName, diag, options)

        dumpTokens(tokens, programName, options)

        // TODO("Parser is not implemented yet")
    }
}

fun koc(
    program: Path,
    diag: Diagnostics = Diagnostics(),
    options: KocOptions,
) = koc(listOf(program), diag, options)

fun kocFromCode(
    programCode: String,
    diag: Diagnostics = Diagnostics(),
    options: KocOptions,
) = kocFromCode(listOf(programCode), diag, options)

private fun dumpTokens(
    tokens: List<Token>,
    sourceId: String,
    options: KocOptions,
) {
    if (!options.dumpTokens) return

    println("-".repeat(6) + " $sourceId " + "-".repeat(6))
    tokens.dump()
}
