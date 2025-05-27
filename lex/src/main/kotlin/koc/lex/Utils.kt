package koc.lex

import koc.core.DiagMessage
import koc.core.Diagnostics
import koc.core.KocOptions
import koc.core.UNDERLINE_CHAR
import java.io.PrintStream


internal val Char.isIdentifier: Boolean
    get() = isLetterOrDigit() || this == '_'

internal val Char.isIdentifierStart: Boolean
    get() = isLetter() || this == '_'

/**
 * Syntactical check for identifier
 */
internal val StringBuilder.isIdentifier: Boolean
    get() {
        if (isEmpty()) return false
        val first = first()
        if (!first.isIdentifierStart) return false

        for (i in 1 ..< length) {
            if (!this[i].isIdentifier) return false
        }
        return true
    }

internal val Char.isIntegerStart: Boolean
    get() = isDigit() || this == '-' || this == '+'

internal val Char.isInteger: Boolean
    get() = isDigit()

/**
 * Syntactical check for integer
 */
internal val StringBuilder.isInteger: Boolean
    get() {
        if (isEmpty()) return false

        if (!first().isIntegerStart) return false

        for (i in 1 ..< length) {
            if (!this[i].isInteger) return false
        }

        return true
    }

internal val Char.isRealStart: Boolean
    get() = isDigit() || this == '-' || this == '+'

internal val Char.isReal: Boolean
    get() = isDigit() || this == '.'

/**
 * Syntactical check for real
 */
internal val StringBuilder.isReal: Boolean
    get() {
        if (isEmpty()) return false

        if (!first().isRealStart) return false
        var hadSeparator = false // Was point met

        for (i in 1 ..< length) {
            if (this[i] == '.') {
                if (hadSeparator) return false
                hadSeparator = true
            }

            if (!this[i].isReal) return false
        }

        return true
    }

fun Collection<Token>.dump(out: PrintStream = System.out) {
    var first = true
    for (token in this) {
        if (!first) out.println()
        out.print(token.toString())
        first = false
    }
}

fun Lexer.Companion.fromOptions(
    opts: KocOptions = KocOptions(), diag: Diagnostics// = Diagnostics()
): Lexer = LexerImpl(diag, opts.stopOnError)


fun Token.asWindow(tokens: List<Token>): Window {
    return Window(this, this, tokens)
}

fun Diagnostics.diag(msg: DiagMessage, token: Token) {
    diag(msg, token.start, token.end)
}

fun Diagnostics.diag(msg: DiagMessage, tokens: Window) {
    diag(msg, tokens.startToken.start, tokens.endToken.end)
}

fun formatTokens(
    window: Window,
    highlight: Window?,
    message: String? = null,
    showLines: Boolean = true,
    showFileName: Boolean = false,
    showHighlightedPos: Boolean = false,
    leadingLines: UInt = 0u,
    trailingLines: UInt = 0u
) = formatTokens(
    window.allTokens, window.start, window.end,
    highlight?.start, highlight?.end, message, showLines, showFileName, showHighlightedPos,
    leadingLines = leadingLines, trailingLines = trailingLines
)
/**
 * @param highlightEnd inclusive position
 */
fun formatTokens(
    tokens: List<Token>,
    start: Int, end: Int,
    highlightStart: Int? = null, highlightEnd: Int? = null,
    message: String? = null,
    showLines: Boolean = true,
    showFileName: Boolean = false,
    showHighlightedPos: Boolean = false,
    leadingLines: UInt = 0u,
    trailingLines: UInt = 0u,
    onlyWindow: Boolean = false
): String {
    if (tokens.isEmpty()) return ""

    require(highlightStart == null && highlightEnd == null || highlightStart != null && highlightEnd != null)
    require(highlightStart == null || highlightStart <= highlightEnd!!)
    require(highlightStart == null || start <= highlightStart && highlightEnd!! <= end)
    require(start <= end || end == -1)
    require(0 <= start && end < tokens.size)

    val res = StringBuilder()

    val lineNoWidth = tokens.lastOrNull()?.end?.line?.toString()?.length ?: 0
    val lineNoIndent = " ".repeat(lineNoWidth + 3)
    var atHighlight = false
    var line = if (tokens.isNotEmpty() && tokens[start].start.line > 0u) tokens[start].start.line.toInt() else 1
    var column = 1
    var firstTokenInLine = true
    var messagePrinted = false

    fun getLastHighlightedTokenIdx(start: Int): Int {
        val token = tokens[start]
        val line = token.start.line
        var end = start
        for (i in start..highlightEnd!!) {
            if (tokens[i].start.line != line) break
            end = i
        }
        return end
    }

    fun getLineHighlightSize(start: Int): UInt {
        var end = getLastHighlightedTokenIdx(start)
        return tokens[end].end.column - tokens[start].start.column + 1u
    }

    fun highlightLine(from: Int) {
        res.appendLine()
        val indent = " ".repeat(tokens[from].start.column.toInt() - 1)
        val lastInLineTokenIdx = getLastHighlightedTokenIdx(from)
        res.append(lineNoIndent).append(indent).append(UNDERLINE_CHAR.repeat(getLineHighlightSize(from).toInt()))

        if (lastInLineTokenIdx == highlightEnd) {
            message?.let { msg ->
                res.appendLine().append(lineNoIndent).append(msg)
            }
            messagePrinted = true
            atHighlight = false
        }
    }

    fun getLineNoPrefix(line: UInt) = "${line.toString().padEnd(lineNoWidth)} | "

    if (tokens.isNotEmpty() && showFileName) {
        res.appendLine("${" ".repeat(lineNoIndent.length - 3)}-> ${tokens.first().start.filename}")
    } else if (tokens.isNotEmpty() && showHighlightedPos) {
        res.appendLine("${" ".repeat(lineNoIndent.length - 3)}-> ${tokens[highlightStart!!].start.toVerboseString()}")
    }

    for (i in if (onlyWindow) start..end else tokens.indices) {
        val token = tokens[i]
        if (token.start.line + leadingLines < tokens[start].start.line) continue
        if (token.end.line > tokens[end].end.line + trailingLines) break
        if (token.start.line > line.toUInt()) {
            // new line
            column = 1

            if (atHighlight) {
                highlightLine(i - 1)
            }

            val lineDiff = token.start.line.toInt() - line
            res.appendLine()
            if (lineDiff > 2) res.append(lineNoIndent).appendLine("...")
            else if (lineDiff == 2) res.appendLine(getLineNoPrefix(token.start.line - 1u))
            firstTokenInLine = true
        }

        if (firstTokenInLine) {
            if (showLines) res.append(getLineNoPrefix(token.start.line))
            firstTokenInLine = false
        }

        if (token.start.column > column.toUInt()) {
            // next token indent
            val indent = " ".repeat(token.start.column.toInt() - column)
            res.append(indent)
        }
        res.append(token.value)
        column = token.end.column.toInt() + 1
        line = token.end.line.toInt()

        if (highlightStart != null && i in highlightStart..highlightEnd!!) {
            atHighlight = true
        }
    }

    if (atHighlight) {
        highlightLine(highlightStart!!)
    }

    if (!messagePrinted && message != null) {
        res.appendLine(message)
    }

    return res.toString()
}
