package koc.ast

import koc.lex.Token
import koc.ast.visitor.Visitor
import koc.parser.impl.ParseScope
import koc.parser.indent
import koc.parser.next
import koc.parser.scope
import koc.parser.tokens
import koc.parser.walk
import koc.core.Position
import koc.lex.Tokens
import koc.lex.Window
import koc.parser.ast.Attribute
import koc.parser.ast.Attributed
import koc.parser.window
import java.util.EnumSet

sealed class Node() : Attributed {
    open val start: Position get() = tokens.first().start
    open val end: Position get() = tokens.last().start

    private val _attributes: MutableSet<Attribute> = EnumSet.noneOf(Attribute::class.java)
    override val attrs: Set<Attribute> get() = _attributes

    private var _scope: ParseScope? = null
    val scope: ParseScope get() = _scope!!

    val isBroken: Boolean get() = Attribute.BROKEN in attrs || tokens.isEmpty() || hasInvalidToken
    val afterSema: Boolean get() = Attribute.AFTER_TYPE_CHECK in attrs
    val isBuiltIn: Boolean get() = Attribute.BUILTIN in attrs
    val inTypeCheck: Boolean get() = Attribute.IN_TYPE_CHECK in attrs
    val inVisit: Boolean get() = Attribute.IN_VISITOR in attrs

    private val hasInvalidToken by lazy {
        tokens.any { !it.isValid }
    }

    protected fun validityCheck() {
        if (hasInvalidToken) {
            enable(Attribute.BROKEN)
        }
    }

    override fun enable(attr: Attribute) {
        _attributes += attr
    }

    override fun disable(attr: Attribute) {
        _attributes -= attr
    }

    protected fun ensureAfterSema() {
        require(afterSema)
    }

    fun specifyScope(scope: ParseScope) {
        if (_scope != null) return
        _scope = scope
    }

    abstract val window: Window
    val tokens: List<Token>
        get() = window.tokens

    private var _allTokens: Tokens? = null
    protected val allTokens: Tokens
        get() = _allTokens!!

    val allCode: List<String>
        get() = _allTokens!!.code

    fun specifyTokens(tokens: Tokens) {
        if (_allTokens != null) return
        _allTokens = tokens
    }

    abstract fun <T> visit(visitor: Visitor<T>): T?

    fun toString(indent: Int): String {
        val builder = StringBuilder()
        toString(indent, builder)
        return builder.toString()
    }

    open fun toString(indent: Int, builder: StringBuilder) {
        builder.append(indent.indent, this.javaClass.simpleName).scope {
            builder.appendLine(tokens.toString().prependIndent(indent.indent))
        }
    }

    override fun toString(): String = toString(0)
}

class File(val filename: String) : Node() {
    private val _decls = mutableListOf<ClassDecl>()
    val decls: List<ClassDecl> get() = _decls

    override val window: Window
        get() = decls.window

    override val start: Position get() = if (decls.isEmpty()) Position(1u, 1u, filename) else super.start
    override val end: Position get() = if (decls.isEmpty()) Position(1u, 1u, filename) else super.end

    override fun <T> visit(visitor: Visitor<T>): T? = walk(
        visitor, visitor.order, visitor.onBroken, *decls.toTypedArray()
    )

    override fun toString(indent: Int, builder: StringBuilder) {
        builder.append(indent.indent, "File(${filename})").scope {
            decls.forEach { it.toString(indent.next, builder) }
        }
    }
}

class GenericParams(
    val lsquare: Token,
    val rsquare: Token
) : Node() {
    private val _types = arrayListOf<TypeParam>()

    val types: List<TypeParam> get() = _types

    operator fun plusAssign(type: TypeParam) {
        _types += type
    }

    operator fun plusAssign(type: Collection<TypeParam>) {
        _types += type
    }

    override fun <T> visit(visitor: Visitor<T>): T? = walk(
        visitor, visitor.order, visitor.onBroken, *types.toTypedArray()
    )

    override val window: Window
        get() = Window(lsquare, rsquare, allTokens.tokens)
}

class ClassBody(val isToken: Token, val endToken: Token) : Node() {
    private val _members = ArrayList<ClassMemberDecl>()

    val members: List<ClassMemberDecl> get() = _members

    operator fun plusAssign(decl: ClassMemberDecl) {
        _members += decl
    }

    operator fun plusAssign(decl: Collection<ClassMemberDecl>) {
        _members += decl
    }

    override fun <T> visit(visitor: Visitor<T>): T? = walk(
        visitor, visitor.order, visitor.onBroken, *members.toTypedArray()
    )

    override val window: Window
        get() = Window(isToken, endToken, allTokens.tokens)
}

/**
 * Formal parameters
 */
class Params(
    val lparenToken: Token,
    val rparenToken: Token,
) : Node() {
    private val _params = ArrayList<Param>()
    val params: List<Param> get() = _params

    operator fun plusAssign(param: Param) {
        _params += param
    }

    operator fun plusAssign(params: Collection<Param>) {
        _params += params
    }

    operator fun get(i: Int): Param = params[i]

    val types: List<ClassType>
        get() = params.map { param -> param.type.classType }

    override fun <T> visit(visitor: Visitor<T>): T? = walk(
        visitor, visitor.order, visitor.onBroken, *params.toTypedArray()
    )

    override val window: Window
        get() = Window(lparenToken, rparenToken, allTokens.tokens)
}

/**
 * Actual parameter
 */
data class Argument(
    val expr: Expr,
    /**
     * comma before the param
     */
    val commaToken: Token? = null
) : Node(), Typed {
    override val type: Type
        get() = expr.type

    override val rootType: ClassType
        get() = expr.rootType

    override val isTypeKnown: Boolean
        get() = expr.isTypeKnown

    override fun specifyType(type: Type) {
        expr.specifyType(type)
    }

    override fun <T> visit(visitor: Visitor<T>): T? = walk(visitor, visitor.order, visitor.onBroken, expr)

    override val window: Window
        get() = Window(expr.tokens.first(), commaToken ?: expr.tokens.last(), allTokens.tokens)
}

sealed class MethodBody(open val node: Node) : Node() {
    class MBody(val body: Body) : MethodBody(body) {
        override val node: Body
            get() = body

        override fun <T> visit(visitor: Visitor<T>): T? = walk(visitor, visitor.order, visitor.onBroken, body)

        override val window: Window
            get() = body.window
    }

    class MExpr(val wideArrow: Token, val expr: Expr) : MethodBody(expr) {
        override val node: Expr
            get() = expr

        override fun <T> visit(visitor: Visitor<T>): T? = walk(visitor, visitor.order, visitor.onBroken, expr)

        override val window: Window
            get() = Window(wideArrow, expr.tokens.last(), allTokens.tokens)
    }

    override fun <T> visit(visitor: Visitor<T>): T? = walk(visitor, visitor.order, visitor.onBroken, node)
}

class Body(val isToken: Token? = null, val endToken: Token) : Node() {
    val _nodes = ArrayList<Node>()
    val nodes: List<Node> get() = _nodes

    operator fun plusAssign(varDecl: VarDecl) {
        _nodes += varDecl
    }

    operator fun plusAssign(statement: Statement) {
        _nodes += statement
    }

    operator fun plusAssign(expr: Expr) {
        _nodes += expr
    }

    override fun <T> visit(visitor: Visitor<T>): T? = walk(
        visitor, visitor.order, visitor.onBroken, *nodes.toTypedArray()
    )

    override val window: Window
        get() = Window(isToken ?: nodes.tokens.firstOrNull() ?: endToken, endToken, allTokens.tokens)
}
