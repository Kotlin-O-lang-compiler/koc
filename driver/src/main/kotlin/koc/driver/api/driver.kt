package koc.driver.api

import koc.driver.Kocpiler
import koc.lex.Token
import koc.lex.dump
import koc.ast.Node
import koc.parser.dump
import koc.core.Diagnostics
import koc.core.KocOptions
import java.nio.file.Path

fun koc(
    programs: List<Path>,
    diag: Diagnostics = Diagnostics(),
    options: KocOptions,
) {
    val compiler = Kocpiler(diag = diag, opts = options)
    for (program in programs) {
        compiler.run(program)
    }
}

fun kocFromCode(
    programs: List<String>,
    diag: Diagnostics = Diagnostics(),
    options: KocOptions,
) {
    val compiler = Kocpiler(diag = diag, opts = options)
    programs.forEachIndexed { idx, program ->
        val programName = "program-$idx"
        compiler.run(program, programName)
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

internal fun dumpTokens(
    tokens: List<Token>,
    sourceId: String,
    options: KocOptions,
) {
    if (!options.dumpTokens) return

    println("-".repeat(6) + " $sourceId " + "-".repeat(6))
    tokens.dump()
}

internal fun dumpParse(
    nodes: List<Node>,
    sourceId: String,
    options: KocOptions,
) {
    if (!options.dumpParse) return

    println("-".repeat(6) + " $sourceId " + "-".repeat(6))
    nodes.forEach(Node::dump)
}
