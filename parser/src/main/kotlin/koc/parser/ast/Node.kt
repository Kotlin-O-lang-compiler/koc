package koc.parser.ast

import koc.lex.Token
import koc.parser.tokens
import koc.utils.Position
import java.util.EnumSet

sealed class Node : Attributed {
    open val start: Position get() = tokens.first().start
    open val end: Position get() = tokens.last().start

    private val _attributes: MutableSet<Attribute> = EnumSet.noneOf(Attribute::class.java)
    override val attrs: Set<Attribute> get() = _attributes

    val isBroken: Boolean get() = Attribute.BROKEN in attrs || hasInvalidToken
    val afterSema: Boolean get() = Attribute.AFTER_TYPE_CHECK in attrs

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

    abstract val tokens: List<Token>
}

class File(val filename: String) : Node() {

    private val _decls = mutableListOf<ClassDecl>()

    val decls: List<ClassDecl> get() = _decls

    override val tokens: List<Token> get() = decls.tokens

    override val start: Position get() = if (decls.isEmpty()) Position(1u, 1u, filename) else super.start
    override val end: Position get() = if (decls.isEmpty()) Position(1u, 1u, filename) else super.end
}

@JvmInline
value class Identifier(val value: String)

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

    override val tokens: List<Token>
        get() = listOf(lsquare) + types.tokens + rsquare
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

    override val tokens: List<Token> get() = listOf(isToken) + members.tokens + endToken
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

    override val tokens: List<Token>
        get() {
            val res = ArrayList<Token>()
            res += lparenToken
            res += params.tokens
            res += rparenToken
            return res
        }
}

data class Argument(
    val expr: Expr,
    /**
     * comma before the param
     */
    val commaToken: Token? = null
) : Node() {
    override val tokens: List<Token>
        get() {
            val res = arrayListOf<Token>()
            res += expr.tokens
            commaToken?.let { res += it }
            return res
        }
}

sealed class MethodBody(open val node: Node): Node() {
    class MBody(val body: Body) : MethodBody(body) {
        override val node: Body
            get() = body

        override val tokens: List<Token>
            get() = body.tokens
    }

    class MExpr(val wideArrow: Token, val expr: Expr) : MethodBody(expr) {
        override val node: Expr
            get() = expr

        override val tokens: List<Token>
            get() = listOf(wideArrow) + expr.tokens
    }
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

    override val tokens: List<Token>
        get() = (isToken?.let { listOf(isToken) } ?: listOf()) + nodes.tokens + endToken
}

//class RefType(
//    val identifierToken: Token,
//    val genericParams: GenericParams? = null
//) : Node(), Typed {
//    override val tokens: List<Token>
//        get() = listOf(identifierToken) + (genericParams?.tokens ?: emptyList())
//
//    private var _type: ClassType? = null
//
//    override val type: Type
//        get() {
//            ensureAfterSema()
//            return _type!!
//        }
//
//    val identifier: Identifier get() = Identifier(identifierToken.value)
//
//    override fun specifyType(type: Type) {
//        require(type is ClassType)
//        _type = type
//    }
//}