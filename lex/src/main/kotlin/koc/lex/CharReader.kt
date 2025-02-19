package koc.lex

import java.io.InputStream
import java.io.InputStreamReader

class CharReader(private val reader: InputStreamReader): Iterator<Char>, AutoCloseable {
    constructor(input: InputStream) : this(input.reader())

    var currentLine = 1u
    private set
    var currentColumn = 1u
    private set

    private var bufferIdx = BUFFER_CAPACITY
    private var bufferSize = BUFFER_CAPACITY
    private val buffer = CharArray(BUFFER_CAPACITY)

    override fun hasNext(): Boolean {
        if (!isBufferEmpty()) return true

        bufferSize = reader.read(buffer)
        if (bufferSize == -1) return false
        bufferIdx = 0
        return true
    }

    override fun next(): Char {
        if (!hasNext()) throw NoSuchElementException()

        val char = buffer[bufferIdx++]
        _prevColumn = currentColumn
        if (char == '\n') {
            currentLine++
            currentColumn = 1u
        } else {
            currentColumn++
        }
        _prevChar = char
        return char
    }

    fun skip(any: Set<Char>) {
        while (hasNext()) {
            val char = next()
            if (char in any) continue
            returnChar()
            break
        }
    }

    private var _prevColumn = 0u // remember column in case of backtracking on lines
    private var _prevChar: Char? = null

    /**
     * Returns previously read character in the buffer.
     * Currently, only 1 char can be returned (next(), returnChar(), returnChar() --- is not allowed)
     */
    fun returnChar() {
        _prevChar?.let {
            buffer[--bufferIdx] = it
            if (it == '\n') {
                currentColumn = _prevColumn
                currentLine--
            } else {
                currentColumn--
            }
        }
        _prevChar = null
    }

    override fun close() {
        reader.close()
    }

    private fun isBufferEmpty() = bufferSize <= bufferIdx

    companion object {
        private const val BUFFER_CAPACITY = 64
    }
}