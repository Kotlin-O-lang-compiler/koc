package koc.ast

import koc.lex.Token
import koc.lex.Window
import koc.ast.visitor.Visitor
import koc.parser.walk

sealed class Statement : Node()

class Assignment(
    val identifierToken: Token,
    val assignmentToken: Token,
    val expr: Expr
) : Statement() {
    override fun <T> visit(visitor: Visitor<T>): T? = walk(visitor, visitor.order, visitor.onBroken, expr)

    override val window: Window
        get() = Window(identifierToken, expr.tokens.last(), allTokens)
}

class ReturnNode(
    val keyword: Token,
    val expr: Expr? = null
) : Statement() {
    override fun <T> visit(visitor: Visitor<T>): T? = walk(visitor, visitor.order, visitor.onBroken, expr)

    override val window: Window
        get() = Window(keyword, expr?.tokens?.last() ?: keyword, allTokens)
}

class WhileNode(
    val keyword: Token,
    val cond: Expr,
    val body: Body, // body with `loop` token as `is`
) : Statement() {
    override fun <T> visit(visitor: Visitor<T>): T? = walk(visitor, visitor.order, visitor.onBroken, cond, body)

    override val window: Window
        get() = Window(keyword, body.tokens.last(), allTokens)
}

class IfNode(
    val ifToken: Token,
    val cond: Expr,
    val thenBody: Body,
    val elseBody: Body? = null,
) : Statement() {
    override fun <T> visit(visitor: Visitor<T>): T? = walk(
        visitor, visitor.order, visitor.onBroken, cond, thenBody, elseBody
    )

    override val window: Window
        get() = Window(ifToken, elseBody?.tokens?.last() ?: thenBody.tokens.last(), allTokens)
}