package koc.parser

import koc.lex.Token
import koc.parser.ast.Node
import koc.parser.ast.visitor.Insight
import koc.parser.ast.visitor.Order
import koc.parser.ast.visitor.Visitor
import koc.parser.impl.ParserImpl
import koc.utils.Diagnostics
import koc.utils.KocOptions
import koc.utils.Position
import java.io.PrintStream

val List<Node>.tokens: List<Token>
    get() {
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

    if (tokens.isEmpty()) {
        message?.let { msg ->
            res.append(msg)
        }
        return res.toString()
    }

    val linNoWidth = tokens.last().end.line.toString().length
    val lineIdent = " ".repeat(linNoWidth + 3)
    val leadingLines = 2u
    var badFound = false
    var line = if (tokens.isNotEmpty() && tokens.first().start.line > 0u) tokens.first().start.line.toInt() else 1
    var column = 1
    var firstTokenInLine = true
    var stopAfterLine = false

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
        if (token.start.line + leadingLines <= bad.start.line) continue
        if (token.start.line > line.toUInt()) {
            column = 1

            if (badFound) {
                onBadFound()
                if (stopAfterLine) break
            }

            val lineDiff = token.start.line.toInt() - line
            if (lineDiff > 1) {
                res.appendLine().append(lineIdent).appendLine("...")
//                res.append("\n".repeat(lineDiff - 1))
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
            stopAfterLine = true
        }
    }

    if (badFound) {
        onBadFound()
    }

    return res.toString()
}

fun Position?.next(file: String = "file"): Position {
    return this?.let {
        it.copy(column = it.column + 1u)
    } ?: Position(1u, 1u, file)
}


val List<Token>.nextPosition: Position
    get() = if (this.isEmpty()) Position(
        1u,
        1u,
        ""
    ) else last().end.copy(column = last().end.column + 2u)

fun Node.dump(out: PrintStream = System.out) {
    out.println(this)
}

private const val INDENT_SIZE = 2

val Int.indent: String
    get() = " ".repeat(INDENT_SIZE * this)

val Int.next: Int
    get() = this + 1

fun <T> StringBuilder.scope(inside: StringBuilder.() -> T): T {
    appendLine("{")
    val res = inside(this)
    appendLine("}")
    return res
}

fun <T: Node, R> T.walk(visitor: Visitor<R>, order: Order = Order.TOP_DOWN, onBroken: Insight, vararg children: Node?): R {
    if (visitor.insight == Insight.STOP) throw IllegalStateException("AST visitor has not stopped properly for ${this.javaClass.simpleName} node")

    var result: R? = null
    if (order == Order.TOP_DOWN) {
        visitor.previsit(this)
        result = visitor.visit(this)
    }

    var childRes: R? = null
    if (isBroken && onBroken == Insight.STOP) visitor.stop()
    for (child in children) {
        if (!visitor.shouldVisitChildren || isBroken && onBroken == Insight.SKIP) break
        childRes = child?.visit(visitor)
    }
    if (visitor.insight == Insight.SKIP) visitor.reset()
    if (isBroken && onBroken == Insight.STOP) visitor.stop()

    if (order == Order.BOTTOM_UP) {
        result = if (visitor.insight != Insight.STOP) {
            visitor.previsit(this)
            visitor.visit(this).also { visitor.postvisit(this, it) }
        } else childRes
    } else {
        visitor.postvisit(this, result!!)
    }

    return result ?: throw IllegalStateException("AST visitor returned no result for ${this.javaClass.simpleName} node")
}

fun Parser.Companion.fromOptions(
    @Suppress("unused") opts: KocOptions = KocOptions(), diag: Diagnostics = Diagnostics()
): Parser = ParserImpl(diag)
