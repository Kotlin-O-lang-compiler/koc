package koc.driver

import koc.lex.Lexer
import koc.lex.LexerImpl
import koc.lex.dump
import koc.utils.Diagnostics
import java.io.File
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    koc(args)
}

fun koc(args: Array<String>) {
    val stopOnError = false // TODO: hide stopOnError under Arguments
    val diag = Diagnostics()
    val (programs, args) = parse(args, diag)

    if (diag.hasErrors) {
        exitProcess(-1)
    }

    koc(programs, args, diag, stopOnError)
}

fun koc(
    programs: List<File>,
    args: Set<Arguments>,
    diag: Diagnostics = Diagnostics(),
    stopOnError: Boolean = false
) {
    val lexer: Lexer = LexerImpl(diag, stopOnError)

    for (program in programs) {
        lexer.open(program)
        val tokens = lexer.lex()
        lexer.close()

        if (Arguments.DumpTokens in args) {
            println("-".repeat(6) + " $program " + "-".repeat(6))
            tokens.dump()
            continue
        }

        TODO("Parser is not implemented yet")
    }
}

fun koc(
    program: File,
    args: Set<Arguments>,
    diag: Diagnostics = Diagnostics(),
    stopOnError: Boolean = false
) = koc(listOf(program), args, diag, stopOnError)

// TODO: avoid code duplication

fun koc(
    programs: List<String>,
    args: Set<Arguments>,
    diag: Diagnostics = Diagnostics(),
    stopOnError: Boolean = false
) {
    val lexer: Lexer = LexerImpl(diag, stopOnError)

    for (program in programs) {
        lexer.open(program)
        val tokens = lexer.lex()
        lexer.close()

        if (Arguments.DumpTokens in args) {
            println("-".repeat(6) + " $program " + "-".repeat(6))
            tokens.dump()
            continue
        }

        TODO("Parser is not implemented yet")
    }
}

fun koc(
    programCode: String,
    args: Set<Arguments>,
    diag: Diagnostics = Diagnostics(),
    stopOnError: Boolean = false
) = koc(listOf(programCode), args, diag, stopOnError)
