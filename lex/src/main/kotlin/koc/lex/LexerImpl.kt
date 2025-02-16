package koc.lex

import koc.utils.Diagnostics
import koc.utils.Position
import java.io.File
import java.io.InputStream
import java.util.LinkedList

/**
 * Single-threaded lexer.
 * The different threads usage behaviour is undefined.
 */
class LexerImpl(
    val diag: Diagnostics,
    val stopOnError: Boolean,
    val separators: List<Char> = Lexer.DEFAULT_TOKEN_SEPARATORS
) : Lexer, Iterator<Token> {
    private var program: InputStream? = null
    private var next: Token? = null
    private var currentLine = 1u
    private var currentColumn = 0u
    private val buffer = StringBuilder()

    override fun open(program: File) {
        checkNotOpened()
        this.program = program.inputStream().buffered()
        parseNextToken()
    }

    override fun open(program: String) {
        checkNotOpened()
        this.program = program.byteInputStream().buffered()
        parseNextToken()
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
        program?.close()
        program = null
    }

    override fun iterator(): Iterator<Token> = this

    override fun hasNext(): Boolean {
        checkOpened()
        return next != null && (!diag.hasErrors || !stopOnError)
    }

    private enum class SupposedToken {
        IDENTIFIER, INTEGER, REAL, OTHER
    }

    override fun next(): Token = parseNextToken()!!

    private fun parseNextToken(): Token? {
        checkOpened()
        val nextToken = next
        var guessToken: SupposedToken? = null

        while (hasNextChar()) {
            val char = nextChar()
            if (char in separators) continue
            returnChar(false)
            break
        }

        val position = Position(currentLine, currentColumn)

        while (hasNextChar()) {
            val char = nextChar()

            if (char in separators) break
            buffer.append(char)

            when {
                guessToken == SupposedToken.IDENTIFIER && buffer.isIdentifier -> continue
                guessToken == SupposedToken.INTEGER && buffer.isInteger -> continue
                guessToken == SupposedToken.REAL && buffer.isReal -> continue

                guessToken == SupposedToken.INTEGER && buffer.isReal -> {
                    guessToken = SupposedToken.REAL
                    continue
                }

                guessToken == SupposedToken.IDENTIFIER
                    || guessToken == SupposedToken.INTEGER
                    || guessToken == SupposedToken.REAL -> {
                        returnChar(true)
                        break
                }

                guessToken == null && buffer.isIdentifier -> guessToken = SupposedToken.IDENTIFIER
                guessToken == null && buffer.isInteger -> guessToken = SupposedToken.INTEGER
                guessToken == null && buffer.isReal -> guessToken = SupposedToken.REAL
                guessToken == null -> guessToken = SupposedToken.OTHER

                else -> {
                    check(guessToken == SupposedToken.OTHER)
                    if (buffer.toString() !in TokenKind.asValues) {
                        returnChar(true)
                        break
                    }
                    continue
                }

            }
        }

        when {
            buffer.isEmpty() -> {
                next = null
            }
            buffer.toString() in TokenKind.asValues -> {
                val kind = TokenKind.fromValue(buffer.toString())
                next = kind.toTokenClass().constructors.first().call(position)
            }
            buffer.isIdentifier -> next = Token.IDENTIFIER(buffer.toString(), position)
            buffer.isInteger -> {
                val absolute = buffer.toString().toBigDecimalOrNull()
                if (absolute == null || absolute > Token.INT_LITERAL.MAX.toBigDecimal() || absolute < Token.INT_LITERAL.MIN.toBigDecimal()) {
                    if (absolute == null) diag.error(UnexpectedTokenException(position, buffer.toString(), expected = listOf(TokenKind.INT_LITERAL)))
                    else diag.error(IntegerLiteralOutOfRangeException(position, buffer.toString()))
                    next = Token.INVALID(buffer.toString(), position)
                } else {
                    next = Token.INT_LITERAL(absolute.toLong(), position)
                }
            }
            buffer.isReal -> {
                val absolute = buffer.toString().toDouble()
                next = Token.REAL_LITERAL(absolute, position)
            }

            else -> {
                diag.error(UnexpectedTokenException(position, buffer.toString()))
                next = Token.INVALID(buffer.toString(), position)
            }
        }

        buffer.clear()
        return nextToken
    }

    private val _chars = LinkedList<Char>()
    private var _currentChar: Char? = null
    private var _prevColumn = 0u // remember column in case of backtracking on lines

    private fun hasNextChar() = _chars.isNotEmpty() || program?.available() != 0

    private fun nextChar(): Char {
        _currentChar = if (_chars.isEmpty()) program!!.read().toChar()
        else _chars.removeFirst()

        if (_currentChar == '\n') {
            currentLine++
            _prevColumn = currentColumn
            currentColumn = 1u
        } else {
            currentColumn++
        }

        return _currentChar!!
    }

    private fun returnChar(removeFromBuffer: Boolean) {
        _currentChar?.let {
            if (removeFromBuffer) buffer.deleteCharAt(buffer.length - 1)
            _chars.addFirst(it)
            if (it == '\n') {
                currentColumn = _prevColumn
                currentLine--
            }
        }
    }

    private fun checkNotOpened() {
        require(program == null)
    }

    private fun checkOpened() {
        require(program != null)
    }
}