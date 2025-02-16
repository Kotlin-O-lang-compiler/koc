package koc.lex

import java.io.File
import java.lang.AutoCloseable

interface Lexer : Iterable<Token>, AutoCloseable {

    fun open(program: File)
    fun open(program: String)

    fun lex(): List<Token>

    companion object {
        val DEFAULT_TOKEN_SEPARATORS = listOf(' ', '\t', '\n', '\r')
    }
}