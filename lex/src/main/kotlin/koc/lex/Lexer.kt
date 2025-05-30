package koc.lex

import java.io.File
import java.lang.AutoCloseable

interface Lexer : Iterable<Token>, AutoCloseable {

    fun open(program: File)
    fun open(program: String, programName: String = "program")

    /**
     * Tokenizes opened program
     */
    fun lex(): Tokens

    fun lex(program: File): Tokens {
        open(program)
        return use {
            lex()
        }
    }
    fun lex(program: String, programName: String = "program"): Tokens {
        open(program, programName)
        return use {
            lex()
        }
    }

    companion object {
        val NLs = setOf('\n')
        val DEFAULT_TOKEN_SEPARATORS = setOf(' ', '\t', '\r') + NLs
    }
}