package koc.parser

import koc.lex.Token
import koc.parser.ast.Attribute
import koc.ast.Node
import koc.ast.Params
import koc.ast.visitor.Insight
import koc.ast.visitor.Order
import koc.ast.visitor.Visitor
import koc.parser.impl.ParserImpl
import koc.core.Diagnostics
import koc.core.KocOptions
import koc.core.Position
import koc.lex.Window
import koc.lex.formatTokens
import java.io.PrintStream

val List<Node>.tokens: List<Token>
    get() {
        val res = arrayListOf<Token>()
        for (node in this) {
            res += node.tokens
        }
        return res
    }

val List<Node>.window: Window
    get() {
        return first().window + last().window
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
        this.enable(Attribute.IN_VISITOR)
        visitor.previsit(this)
        result = visitor.visit(this)
    }

    var childRes: R? = null
    if (isBroken && onBroken == Insight.STOP || result is Insight && result == Insight.STOP) visitor.stop()
    for (child in children) {
        if (!visitor.shouldVisitChildren || isBroken && onBroken == Insight.SKIP || result is Insight && result == Insight.SKIP) break
        childRes = child?.visit(visitor)
        if (childRes is Insight && childRes == Insight.STOP) visitor.stop()
    }
    if (visitor.insight == Insight.SKIP) visitor.reset()
    if (isBroken && onBroken == Insight.STOP) visitor.stop()

    if (order == Order.BOTTOM_UP) {
        result = if (visitor.insight != Insight.STOP) {
            this.enable(Attribute.IN_VISITOR)
            visitor.previsit(this)
            visitor.visit(this).also {
                visitor.postvisit(this, it)
                this.disable(Attribute.IN_VISITOR)
            }
        } else childRes
    } else {
        visitor.postvisit(this, result!!)
        this.disable(Attribute.IN_VISITOR)
    }

    return result ?: throw IllegalStateException("AST visitor returned no result for ${this.javaClass.simpleName} node")
}

fun Parser.Companion.fromOptions(
    @Suppress("unused") opts: KocOptions = KocOptions(), diag: Diagnostics// = Diagnostics()
): Parser = ParserImpl(diag)

fun Diagnostics.load(tokens: List<Token>) {
    val code = formatTokens(tokens, 0, tokens.size - 1, showLines = false)
    load(code)
}

fun Diagnostics.loadIfEmpty(tokens: List<Token>) {
    val code = formatTokens(tokens, 0, tokens.size - 1, showLines = false)
    loadIfEmpty(code)
}

val Params?.size: Int
    get() = this?.params?.size ?: 0