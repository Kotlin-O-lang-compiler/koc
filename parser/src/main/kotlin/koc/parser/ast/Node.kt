package koc.parser.ast

import koc.lex.Token
import koc.parser.tokens
import koc.utils.Position
import java.util.EnumSet

sealed class Node {
//    constructor(position: Position) : this(position, position)

    open val start: Position get() = tokens.first().start
    open val end: Position get() = tokens.last().start

    private val _attributes: MutableSet<Attribute> = EnumSet.noneOf(Attribute::class.java)
    val attrs: Set<Attribute> get() = _attributes

    val isBroken: Boolean = Attribute.BROKEN in attrs
    val afterSema: Boolean = Attribute.AFTER_TYPE_CHECK in attrs

    fun enable(attr: Attribute) {
        _attributes += attr
    }

    fun disable(attr: Attribute) {
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

class ClassReference(val identifierToken: Token) : Node(), Typed {
    val identifier: Identifier get() = Identifier(identifierToken.value)

    private var _type: ClassType? = null

    /**
     * Type of reference destination
     */
    override val type: ClassType
        get() {
            ensureAfterSema()
            return _type!!
        }

    override val tokens: List<Token>
        get() = listOf(identifierToken)

    override fun specifyType(type: Type) {
        require(type is ClassType)
        _type = type
    }

}

class ClassBody() : Node() {

    private val _members = ArrayList<ClassMemberDecl>()

    val members: List<ClassMemberDecl> get() = _members

    override val tokens: List<Token> get() = members.tokens
}

class Params(
    val lparenToken: Token,
    val rparenToken: Token,
) : Node() {
    private val _params = ArrayList<Param>()
    val params: List<Param> get() = _params

    operator fun plusAssign(param: Param) {
        _params += param
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

sealed class MethodBody(open val node: Node): Node() {
    class MBody(val body: Body) : MethodBody(body) {
        override val node: Body
            get() = body

        override val tokens: List<Token>
            get() = body.tokens
    }

    class MExpr(val expr: Expr) : MethodBody(expr) {
        override val node: Expr
            get() = expr

        override val tokens: List<Token>
            get() = expr.tokens
    }
}

class Body() : Node() {
    val _nodes = ArrayList<Node>()
    val nodes: List<Node> get() = _nodes

    operator fun plusAssign(varDecl: VarDecl) {
        _nodes += varDecl
    }

    operator fun plusAssign(statement: Statement) {
        _nodes += statement
    }

    override val tokens: List<Token>
        get() = nodes.tokens
}
