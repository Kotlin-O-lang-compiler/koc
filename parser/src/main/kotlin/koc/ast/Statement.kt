package koc.parser.ast

import koc.lex.Token
import koc.parser.ast.visitor.Visitor
import koc.parser.walk

sealed class Statement : Node()

class Assignment(
    val identifierToken: Token,
    val assignmentToken: Token,
    val expr: Expr
) : Statement() {
    override fun <T> visit(visitor: Visitor<T>): T? = walk(visitor, visitor.order, visitor.onBroken, expr)

    override val tokens: List<Token>
        get() {
            val res = arrayListOf<Token>()
            res += identifierToken
            res += assignmentToken
            res += expr.tokens
            return res
        }
}

class ReturnNode(
    val keyword: Token,
    val expr: Expr? = null
) : Statement() {
    override fun <T> visit(visitor: Visitor<T>): T? = walk(visitor, visitor.order, visitor.onBroken, expr)

    override val tokens: List<Token>
        get() {
            val res = arrayListOf<Token>()
            res += keyword
            expr?.let { res += it.tokens }
            return res
        }
}

class WhileNode(
    val keyword: Token,
    val cond: Expr,
    val body: Body, // body with `loop` token as `is`
) : Statement() {
    override fun <T> visit(visitor: Visitor<T>): T? = walk(visitor, visitor.order, visitor.onBroken, cond, body)

    override val tokens: List<Token>
        get() = listOf(keyword) + cond.tokens + body.tokens
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

    override val tokens: List<Token>
        get() {
            val res = arrayListOf<Token>()
            res += ifToken
            res += cond.tokens
            res += thenBody.tokens
            elseBody?.let { res += it.tokens }
            return res
        }
}