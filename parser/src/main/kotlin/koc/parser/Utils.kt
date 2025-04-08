package koc.parser

import koc.lex.Token
import koc.parser.ast.Node
import koc.utils.Position

val List<Node>.tokens: List<Token> get() {
    val res = arrayListOf<Token>()
    for (node in this) {
        res += node.tokens
    }
    return res
}

private const val UNDERLINE_CHAR = "~"

// TODO: add case when bad token is not in tokens (like expected token but absent)
fun formatAsBadToken(bad: Token, tokens: List<Token>, message: String? = null): String {
    val res = StringBuilder()

    if (tokens.isEmpty() || tokens.size == 1) {
        res.append("${bad.start.line} | ")
        val lineIdent = bad.start.line.toString().length + 3
        val ident = " ".repeat(bad.start.column.toInt() - 1)
        res.append(ident)
        res.appendLine(bad.value)
        res.append(ident).append(" ".repeat(lineIdent))
        res.appendLine(UNDERLINE_CHAR.repeat(bad.length))
        message?.let { msg ->
            res.appendLine(msg)
        }
        return res.toString()
    }

    val linNoWidth = tokens.last().end.line.toString().length
    val lineIdent = " ".repeat(linNoWidth + 3)
    var badFound = false
    var line = 1
    var column = 1
    var firstTokenInLine = true

    fun onBadFound() {
        res.appendLine()
        val ident = " ".repeat(bad.start.column.toInt() - 1)
        res.append(lineIdent).append(ident).append(UNDERLINE_CHAR.repeat(bad.length)).appendLine()

        message?.let { msg ->
            res.appendLine(msg)
        }
        badFound = false
    }

    for (token in tokens) {
        if (token.start.line > line.toUInt()) {
            column = 1

            if (badFound) {
                onBadFound()
            }

            val lineDiff = token.start.line.toInt() - line
            if (lineDiff > 1) {
                res.append(lineIdent).appendLine("...")
                res.append("\n".repeat(lineDiff - 1))
            } else {
                res.appendLine()
            }
            firstTokenInLine = true
        }

        if (firstTokenInLine) {
            res.append("${token.start.line} | ")
            firstTokenInLine = false
        }

        if (token.start.column > column.toUInt()) {
            val ident = " ".repeat(token.start.column.toInt() - column)
            res.append(ident)
        }
        res.append(token.value)
        column = token.end.column.toInt() + 1
        line = token.end.line.toInt()

        if (bad == token) {
            badFound = true
        }
    }

    if (badFound) {
        onBadFound()
    }

    return res.toString()
}

val List<Token>.nextPosition: Position get() = if (this.isEmpty()) Position(1u, 1u, "") else last().end.copy(column = last().end.column + 2u)