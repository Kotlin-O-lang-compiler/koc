package koc.lex

import koc.utils.Diagnostics
import koc.utils.Position
import java.io.File

/**
 * Single-threaded lexer.
 * The different threads usage behaviour is undefined.
 */
class LexerImpl(
    val diag: Diagnostics,
    val stopOnError: Boolean,
    val separators: Set<Char> = Lexer.DEFAULT_TOKEN_SEPARATORS
) : Lexer, Iterator<Token> {
    private var _reader: CharReader? = null
    private lateinit var sourceName: String

    private var next: Token? = null
    private val buffer = StringBuilder()

    private val reader: CharReader get() = _reader!!

    override fun open(program: File) {
        checkNotOpened()
        this._reader = CharReader(program.inputStream())
        sourceName = program.name
    }

    override fun open(program: String, programName: String) {
        checkNotOpened()
        this._reader = CharReader(program.byteInputStream())
        sourceName = programName
    }

    override fun lex(): List<Token> {
        checkOpened()
        val tokens = arrayListOf<Token>()
        for (token in this) {
            tokens += token
        }
        return tokens
    }

    override fun close() {
        _reader?.close()
        _reader = null
    }

    override fun iterator(): Iterator<Token> = this

    override fun hasNext(): Boolean {
        checkOpened()
        if (next == null && (!diag.hasErrors || !stopOnError)) parseNextToken()
        return next != null && (!diag.hasErrors || !stopOnError)
    }

    private enum class LexerState {
        NONE, IDENTIFIER, INTEGER, REAL, SPECIAL
    }

    override fun next(): Token {
        if (next == null) throw NoSuchElementException()
        val current = next!!
        next = null
        return current
    }

    private fun parseTokenChars(): LexerState {
        var state = LexerState.NONE

        for (char in reader) {
            if (char in separators) break
            buffer.append(char)

            when {
                state == LexerState.IDENTIFIER && char.isIdentifier -> continue
                state == LexerState.INTEGER && char.isInteger -> continue
                state == LexerState.REAL && char.isInteger /* fractional part */ -> continue

                state == LexerState.INTEGER && buffer[buffer.length - 2].isInteger && char == '.' -> {
                    state = LexerState.REAL
                    continue
                }

                state == LexerState.IDENTIFIER
                    || state == LexerState.INTEGER
                    || state == LexerState.REAL -> {
                    reader.returnChar()
                    buffer.deleteCharAt(buffer.length - 1)
                    break
                }

                state == LexerState.NONE && char.isIdentifierStart -> state = LexerState.IDENTIFIER
                state == LexerState.NONE && char.isIntegerStart -> state = LexerState.INTEGER
                state == LexerState.NONE && char.isRealStart -> state = LexerState.REAL // Redundant branch due to integer rule equality

                else -> {
                    check(state == LexerState.NONE || state == LexerState.SPECIAL)

                    // Looks like it's not frequent call for valid programs
                    if (buffer.toString() !in TokenKind.asValues) {
                        reader.returnChar()
                        buffer.deleteCharAt(buffer.length - 1)

                        break
                    }
                    state = LexerState.SPECIAL
                    continue
                }

            }
        }
        return state
    }

    private fun parseNextToken() {
        checkOpened()
        reader.skip(separators)

        val position = Position(reader.currentLine, reader.currentColumn, sourceName)

        val state = parseTokenChars()

        when {
            buffer.isEmpty() -> {
                next = null
            }
            (state == LexerState.IDENTIFIER || state == LexerState.SPECIAL) && buffer.toString() in TokenKind.asValues -> {
                val kind = TokenKind.fromValue(buffer.toString())
                next = kind.toTokenClass().constructors.first().call(position)
            }
            state == LexerState.IDENTIFIER -> next = Token.IDENTIFIER(buffer.toString(), position)
            state == LexerState.INTEGER -> {
                val absolute = buffer.toString().toBigDecimalOrNull()
                if (absolute == null || absolute > Token.INT_LITERAL.MAX.toBigDecimal() || absolute < Token.INT_LITERAL.MIN.toBigDecimal()) {
                    if (absolute == null) diag.error(UnexpectedTokenException(buffer.toString(), expected = listOf(TokenKind.INT_LITERAL)), position)
                    else diag.error(IntegerLiteralOutOfRangeException(position, buffer.toString()), position)
                    next = Token.INVALID(buffer.toString(), position)
                } else {
                    next = Token.INT_LITERAL(absolute.toLong(), position)
                }
            }
            state == LexerState.REAL -> {
                val absolute = buffer.toString().toDouble()
                next = Token.REAL_LITERAL(absolute, position)
            }

            else -> {
                diag.error(UnexpectedTokenException(buffer.toString()), position)
                next = Token.INVALID(buffer.toString(), position)
            }
        }

        buffer.clear()
    }

    private fun checkNotOpened() {
        require(_reader == null)
    }

    private fun checkOpened() {
        require(_reader != null)
    }
}