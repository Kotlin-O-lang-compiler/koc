package koc.parser.ast

import koc.lex.Token
import koc.lex.TokenKind
import koc.parser.tokens


sealed class Expr : Node(), Typed

class InvalidExpr : Expr() {
    init {
        enable(Attribute.BROKEN)
    }

    override val tokens: List<Token>
        get() = listOf()

    private var _type: Type = InvalidType()

    override val type: Type get() = _type

    override fun specifyType(type: Type) {
        _type = type
    }
}

class RefExpr(val identifierToken: Token, val generics: GenericParams? = null) : Expr(), Typed {
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
        get() = listOf(identifierToken) + (generics?.tokens ?: listOf())

    override fun specifyType(type: Type) {
        require(type is ClassType)
        _type = type
    }
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

    override fun specifyType(type: Type) {
        require(type is ClassType)
        _type = type
    }
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

    override fun specifyType(type: Type) {
        require(type is ClassType)
        _type = type
    }
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

    override fun specifyType(type: Type) {
        require(type is ClassType)
        _type = type
    }
}

class ThisExpr(val token: Token) : Expr() {
    override val tokens: List<Token> get() = listOf(token)

    init {
        validityCheck()
    }

    private var _type: ClassType? = null

    override val type: ClassType
        get() {
            ensureAfterSema()
            return _type!!
        }

    override fun specifyType(type: Type) {
        require(type is ClassType)
        _type = type
    }
}

class CallExpr(
    val ref: RefExpr,
    val lparen: Token,
    val rparen: Token
) : Expr() {

    private val _args = ArrayList<Expr>()

    val args: List<Expr> get() = _args

    operator fun plusAssign(arg: Expr) {
        _args += arg
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

    override fun specifyType(type: Type) {
        require(type is ClassType)
        _type = type
    }
}

class MemberAccessExpr(
    val left: RefExpr,
    val member: Expr
) : Expr() {
    init {
        require(member is CallExpr || member is RefExpr || member is MemberAccessExpr)
    }

    val isCall: Boolean get() = member is CallExpr || (member is MemberAccessExpr && member.isCall)

    override val tokens: List<Token>
        get() = left.tokens + member.tokens

    private var _type: ClassType? = null
    override val type: Type
        get() {
            ensureAfterSema()
            return _type!!
        }

    override fun specifyType(type: Type) {
        require(type is ClassType)
        _type = type
    }
}