package koc.lex

import java.io.File
import java.lang.AutoCloseable

interface Lexer : Iterable<Token>, AutoCloseable {

    fun open(program: File)
    fun open(program: String, programName: String = "program")

    fun lex(): List<Token>

    companion object {
        val NLs = setOf('\n')
        val DEFAULT_TOKEN_SEPARATORS = setOf(' ', '\t', '\r') + NLs
    }
}