package koc.lex

import koc.core.Diagnostics
import koc.core.Position
import koc.lex.diag.IntegerLiteralOutOfRange
import koc.lex.diag.UnexpectedToken
import java.io.File

/**
 * Single-threaded lexer.
 * The different threads usage behaviour is undefined.
 */
class LexerImpl(
    val diag: Diagnostics,
    val stopOnError: Boolean,
    val separators: Set<Char> = Lexer.DEFAULT_TOKEN_SEPARATORS,
    val newLines: Set<Char> = Lexer.NLs
) : Lexer, Iterator<Token> {
    private var _reader: CharReader? = null
    private lateinit var sourceName: String

    private var next: Token? = null
    private val buffer = StringBuilder()

    private val reader: CharReader get() = _reader!!

    private var readCode: () -> List<String> = { emptyList() }

    override fun open(program: File) {
        checkNotOpened()
        this._reader = CharReader(program.inputStream())
        readCode = { program.inputStream().bufferedReader().readLines() }
        sourceName = program.name
    }

    override fun open(program: String, programName: String) {
        checkNotOpened()
        this._reader = CharReader(program.byteInputStream())
        readCode = { program.lines() }
        sourceName = programName
    }

    override fun lex(): Tokens {
        checkOpened()
        val tokens = arrayListOf<Token>()
        for (token in this) {
            tokens += token
        }
        return Tokens(tokens, readCode)
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
        parseNextToken()
        return current
    }

    private fun parseTokenChars(): LexerState {
        var state = LexerState.NONE

        for (char in reader) {
            if (char in separators) {
                reader.returnChar()
                break
            }
            buffer.append(char)

            when {
                state == LexerState.IDENTIFIER && char.isIdentifier -> continue
                state == LexerState.INTEGER && char.isInteger -> continue
                state == LexerState.REAL && (char.isInteger || char == '.') /* fractional part */ -> continue

                state == LexerState.INTEGER && buffer[buffer.length - 2].isInteger && char == '.' -> {
                    state = LexerState.REAL
                    continue
                }

                state == LexerState.IDENTIFIER
                        || state == LexerState.INTEGER
                        || state == LexerState.REAL -> {
                            if (char.isIdentifier) continue
                            reader.returnChar()
                            buffer.deleteCharAt(buffer.length - 1)
                            break
                }

                state == LexerState.NONE && char.isIdentifierStart -> state = LexerState.IDENTIFIER
                state == LexerState.NONE && char.isIntegerStart -> state = LexerState.INTEGER
                state == LexerState.NONE && char.isRealStart -> state =
                    LexerState.REAL // Redundant branch due to integer rule equality

                else -> {
                    check(state == LexerState.NONE || state == LexerState.SPECIAL)

                    if (buffer.length == TokenKind.COMMENT.value.length && buffer.toString() == TokenKind.COMMENT.value) {
                        return LexerState.SPECIAL
                    }

                    // Looks like it's not frequent call for valid programs
                    if (buffer.length > 1 && buffer.toString() !in TokenKind.asValues && buffer.subSequence(0..<buffer.lastIndex) in TokenKind.asValues) {
                        reader.returnChar()
                        buffer.deleteCharAt(buffer.length - 1)
                        break
                    }
                    state = LexerState.SPECIAL
                    continue
                }

            }
        }

        if (state == LexerState.SPECIAL && buffer.toString() !in TokenKind.asValues) {
            state = LexerState.NONE
        }
        return state
    }

    private fun parseSingleLineComment() {
        for (char in reader) {
            if (char in newLines) break
            buffer.append(char)
        }
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
                if (kind == TokenKind.COMMENT) {
                    parseSingleLineComment()
                    next = Token(buffer.toString(), kind, position)
                } else {
                    next = Token(kind, position)
                }
            }
            state == LexerState.IDENTIFIER -> next = Token(buffer.toString(), TokenKind.IDENTIFIER, position)
            state == LexerState.INTEGER -> {
                val absolute = buffer.toString().toBigDecimalOrNull()
                if (absolute == null || absolute > Token.INT_MAX.toBigDecimal() || absolute < Token.INT_MIN.toBigDecimal()) {
                    if (absolute == null) diag.diag(
                        UnexpectedToken(buffer.toString(), listOf(TokenKind.INT_LITERAL), readCode()), position,
                        position.plus(columns = buffer.length.toUInt() - 1u)
                    )
                    else diag.diag(IntegerLiteralOutOfRange(buffer.toString(), readCode()), position, position.plus(columns = buffer.length.toUInt() - 1u))
                    next = Token(buffer.toString(), TokenKind.INVALID, position)
                } else {
                    next = Token(absolute.toString(), TokenKind.INT_LITERAL, position)
                }
            }

            state == LexerState.REAL -> {
                val absolute = buffer.toString().toDoubleOrNull()
                if (absolute == null) {
                    diag.diag(UnexpectedToken(buffer.toString(), listOf(TokenKind.REAL_LITERAL), readCode()), position, position.plus(columns = buffer.length.toUInt() - 1u))
                    next = Token(buffer.toString(), TokenKind.INVALID, position)
                } else {
                    next = Token(absolute.toString(), TokenKind.REAL_LITERAL, position)
                }
            }

            else -> {
                diag.diag(UnexpectedToken(buffer.toString(), code = readCode()), position, position.plus(columns = buffer.length.toUInt() - 1u))
                next = Token(buffer.toString(), TokenKind.INVALID, position)
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