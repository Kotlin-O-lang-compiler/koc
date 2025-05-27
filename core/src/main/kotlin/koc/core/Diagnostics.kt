package koc.core

import java.io.PrintStream

class Diagnostics(/*code: String = "",*/ private val outstream: PrintStream = System.out, private val errstream: PrintStream = System.err) {
    private val diags = mutableListOf<DiagMessage>()
    val diagnostics: List<DiagMessage> get() = diags

    val hasErrors get() = diags.any { diag -> diag.verbosity == DiagKind.Verbosity.ERROR }
    val hasWarnings get() = diags.any { diag -> diag.verbosity == DiagKind.Verbosity.ERROR }
    val hasAny get() = diags.isNotEmpty()

    inline fun <reified T : DiagKind> has(): Boolean {
        return diagnostics.any { it.kind is T }
    }

//    private var code = code.lines()

//    fun load(code: String) {
//        clear()
//        this.code = code.lines()
//    }

//    fun loadIfEmpty(code: String) {
//        if (diags.isEmpty() && (this.code.isEmpty() || this.code.size == 1 && this.code.first().isEmpty())) {
//            load(code)
//        }
//    }

    private inline fun <T> add(msg: DiagMessage, action: Diagnostics.(out: PrintStream) -> T): T {
        diags += msg
        val out = if (msg.verbosity == DiagKind.Verbosity.INFO) outstream else errstream
        if (diags.isNotEmpty()) out.println()

        out.println("${msg.verbosity.asString()}: $msg")
        val res = this.action(out)
        return res
    }

    fun diag(msg: DiagMessage, pos: Position) = add(msg) { out ->
        out.println(formatCode(msg.code, pos, pos, pos, pos, msg.extraMessage, leadingLines = 1u, trailingLines = 0u))
    }

    fun diag(msg: DiagMessage, start: Position, end: Position) = add(msg) { out ->
        out.println(formatCode(msg.code, start, end, start, end, msg.extraMessage, leadingLines = 1u, trailingLines = 0u, showHighlightedPos = true))
    }

    fun clear() {
        diags.clear()
    }
}


const val UNDERLINE_CHAR = "~"

fun formatCode(
    code: List<String>,
    start: Position, end: Position,
    highlightStart: Position? = null, highlightEnd: Position? = null,
    message: String? = null,
    showLines: Boolean = true,
    showFileName: Boolean = false,
    showHighlightedPos: Boolean = false,
    leadingLines: UInt = 0u,
    trailingLines: UInt = 0u
): String {
    if (code.isEmpty() || code.size == 1 && code.first().isEmpty()) return ""

    require(highlightStart == null && highlightEnd == null || highlightStart != null && highlightEnd != null)
    require(highlightStart == null || highlightStart <= highlightEnd!!)
    require(highlightStart == null || start <= highlightStart && highlightEnd!! <= end)
    require(start <= end)
//    require(Position(1u, 1u) <= start && end <= Position(code.lastIndex.toUInt() + 1u, code.last().lastIndex.toUInt() + 1u))
    require(Position(1u, 1u) <= start && end <= Position(code.lastIndex.toUInt() + 1u, UInt.MAX_VALUE))
    require(showHighlightedPos == false || highlightStart != null)

    val res = StringBuilder()

    val lineNoWidth = code.lastIndex.takeIf { it >= 0 }?.toString()?.length ?: 0
    val lineNoIndent = " ".repeat(lineNoWidth + 3)
    var atHighlight = false
    var line = if (start.line - leadingLines > 0u) start.line - leadingLines else 1u
    var column = 1
    var firstTokenInLine = true
    var messagePrinted = false

    fun getLastHighlightedPosInLine(start: Position): Position {
        if (start.line == highlightEnd!!.line) return highlightEnd
        return Position(start.line, code[start.line.toInt() - 1].lastIndex.toUInt() + 1u)
    }

    fun getLineHighlightSize(start: Position): UInt {
        var end = getLastHighlightedPosInLine(start)
        return end.column - start.column + 1u
    }

    fun highlightLine(from: Position) {
        res.appendLine()
        val fromColumn = if (from.line == highlightStart!!.line) highlightStart.column else from.column
        val indent = " ".repeat(fromColumn.toInt() - 1)
        val lastInLinePos = getLastHighlightedPosInLine(from.copy(column = fromColumn))
        res.append(lineNoIndent).append(indent).append(UNDERLINE_CHAR.repeat(getLineHighlightSize(from.copy(column = fromColumn)).toInt()))

        if (lastInLinePos == highlightEnd) {
            message?.let { msg ->
                res.appendLine().append(lineNoIndent).append(msg)
            }
            messagePrinted = true
            atHighlight = false
        }
    }

    fun getLineNoPrefix(line: UInt) = "${line.toString().padEnd(lineNoWidth)} | "

    if (code.isNotEmpty() && showFileName) {
        res.appendLine("${" ".repeat(lineNoIndent.length - 3)}-> ${start.filename}")
    } else if (code.isNotEmpty() && showHighlightedPos) {
        res.appendLine("${" ".repeat(lineNoIndent.length - 3)}-> ${highlightStart!!.toVerboseString()}")
    }

    for (lineIdx in code.indices) {
        val i = lineIdx + 1
        if (i.toUInt() + leadingLines < start.line) continue
        if (i.toUInt() > end.line + trailingLines) break
        if (i.toUInt() > line) {
            // new line
            column = 1

            if (atHighlight) {
                highlightLine(Position(lineIdx.toUInt(), column.toUInt()))
            }

            val lineDiff = i.toUInt() - line
            res.appendLine()
            if (lineDiff > 2u) res.append(lineNoIndent).appendLine("...")
            else if (lineDiff == 2u) res.appendLine(getLineNoPrefix(i.toUInt() - 1u))
            firstTokenInLine = true
        }

        if (firstTokenInLine) {
            if (showLines) res.append(getLineNoPrefix(i.toUInt()))
            firstTokenInLine = false
        }

        res.append(code[lineIdx])
        line = i.toUInt()

        if (highlightStart != null && i.toUInt() in highlightStart.line..highlightEnd!!.line) {
            atHighlight = true
        }
    }

    if (atHighlight) {
        highlightLine(if (highlightStart!!.line == highlightEnd!!.line) highlightStart else Position(highlightEnd.line, 1u))
    }

    if (!messagePrinted && message != null) {
        res.appendLine(message)
    }

    return res.toString()
}