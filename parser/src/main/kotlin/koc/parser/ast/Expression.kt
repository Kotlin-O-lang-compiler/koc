package koc.parser.ast

import koc.lex.Token
import koc.lex.TokenKind
import koc.parser.ast.visitor.Visitor
import koc.parser.tokens
import koc.parser.walk


sealed class Expr : Node(), Typed

class InvalidExpr : Expr() {
    init {
        enable(Attribute.BROKEN)
    }

    override val tokens: List<Token>
        get() = listOf()

    private var _type: ClassType? = null

    override val type: ClassType
        get() {
            ensureAfterSema()
            return _type!!
        }

    override val rootType: ClassType
        get() = type

    override fun specifyType(type: Type) {
        require(type is ClassType)
        _type = type
    }

    override fun <T> visit(visitor: Visitor<T>): T? = walk(visitor, visitor.order, visitor.onBroken)
}

data class RefExpr(val identifierToken: Token, val generics: GenericParams? = null) : Expr(), Typed {
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

    override val rootType: ClassType
        get() = type

    val isThis: Boolean
        get() = identifierToken.kind == TokenKind.THIS

    override val tokens: List<Token>
        get() = listOf(identifierToken) + (generics?.tokens ?: listOf())

    override fun specifyType(type: Type) {
        require(type is ClassType)
        _type = type
    }

    override fun <T> visit(visitor: Visitor<T>): T? = walk(visitor, visitor.order, visitor.onBroken, generics)
}

class IntegerLiteral(val token: Token) : Expr() {
    val value: Long
        get() {
            require(token.kind == TokenKind.INT_LITERAL)
            return token.value.toLong()
        }

    override val tokens: List<Token> get() = listOf(token)

    private var _type: ClassType? = null

    override val type: ClassType
        get() {
            ensureAfterSema()
            return _type!!
        }

    override val rootType: ClassType
        get() = type

    override fun specifyType(type: Type) {
        require(type is ClassType)
        _type = type
    }

    override fun <T> visit(visitor: Visitor<T>): T? = walk(visitor, visitor.order, visitor.onBroken)
}

class RealLiteral(val token: Token) : Expr() {
    val value: Double
        get() {
            require(token.kind == TokenKind.REAL_LITERAL)
            return token.value.toDouble()
        }

    override val tokens: List<Token> get() = listOf(token)

    private var _type: ClassType? = null

    override val type: ClassType
        get() {
            ensureAfterSema()
            return _type!!
        }

    override val rootType: ClassType
        get() = type

    override fun specifyType(type: Type) {
        require(type is ClassType)
        _type = type
    }

    override fun <T> visit(visitor: Visitor<T>): T? = walk(visitor, visitor.order, visitor.onBroken)
}

class BooleanLiteral(val token: Token) : Expr() {
    val value: Boolean
        get() {
            require(token.kind == TokenKind.TRUE || token.kind == TokenKind.FALSE)
            return token.kind == TokenKind.TRUE
        }

    override val tokens: List<Token> get() = listOf(token)

    private var _type: ClassType? = null

    override val type: ClassType
        get() {
            ensureAfterSema()
            return _type!!
        }

    override val rootType: ClassType
        get() = type

    override fun specifyType(type: Type) {
        require(type is ClassType)
        _type = type
    }

    override fun <T> visit(visitor: Visitor<T>): T? = walk(visitor, visitor.order, visitor.onBroken)
}

class CallExpr(val ref: RefExpr, val lparen: Token, val rparen: Token) : Expr() {
    private val _args = ArrayList<Argument>()

    val args: List<Argument> get() = _args

    operator fun plusAssign(arg: Expr) {
        _args += Argument(arg)
    }

    operator fun plusAssign(arg: Argument) {
        _args += arg
    }

    operator fun plusAssign(arg: Pair<Expr, Token>) {
        val (expr, comma) = arg
        _args += Argument(expr, comma)
    }

    operator fun plusAssign(args: List<Argument>) {
        _args += args
    }

    val isConstructorCall: Boolean get() = Attribute.CONSTRUCTOR_CALL in attrs

    override val tokens: List<Token>
        get() = ref.tokens + lparen + args.tokens + rparen

    private var _type: ClassType? = null

    override val type: ClassType
        get() {
            ensureAfterSema()
            return _type!!
        }

    override val rootType: ClassType
        get() = type

    override fun specifyType(type: Type) {
        require(type is ClassType)
        _type = type
    }

    override fun <T> visit(visitor: Visitor<T>): T? = walk(
        visitor, visitor.order, visitor.onBroken, ref, *args.toTypedArray()
    )
}

class MemberAccessExpr(
    val left: RefExpr,
    val dot: Token,
    val member: Expr
) : Expr() {
    init {
        require(member is CallExpr || member is RefExpr || member is MemberAccessExpr)
    }

    val isCall: Boolean get() = member is CallExpr || (member is MemberAccessExpr && member.isCall)

    override val tokens: List<Token>
        get() = left.tokens + dot + member.tokens

    private var _type: ClassType? = null

    override val type: ClassType
        get() {
            ensureAfterSema()
            return _type!!
        }

    override val rootType: ClassType
        get() = type

    override fun specifyType(type: Type) {
        require(type is ClassType)
        _type = type
    }

    override fun <T> visit(visitor: Visitor<T>): T? = walk(visitor, visitor.order, visitor.onBroken, left, member)
}