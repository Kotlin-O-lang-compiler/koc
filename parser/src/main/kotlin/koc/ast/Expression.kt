package koc.ast

import koc.ast.visitor.Visitor
import koc.lex.Token
import koc.lex.TokenKind
import koc.lex.Window
import koc.lex.asWindow
import koc.parser.ast.Attribute
import koc.parser.walk


sealed class Expr : Node(), Typed {
    val isThis: Boolean
        get() = (this as? RefExpr)?.identifierToken?.kind == TokenKind.THIS
}

class InvalidExpr : Expr() {
    init {
        enable(Attribute.BROKEN)
    }

    override val window: Window
        get() = Window(0, 0, emptyList())

    private var _type: ClassType? = null

    override val type: ClassType
        get() {
            return _type!!
        }

    override val isTypeKnown: Boolean
        get() = _type != null

    override val isRootTypeKnown: Boolean
        get() = isTypeKnown

    override val rootType: ClassType
        get() = type

    override fun specifyType(type: Type) {
        require(type is ClassType)
        _type = type
    }

    override fun <T> visit(visitor: Visitor<T>): T? = walk(visitor, visitor.order, visitor.onBroken)
}

data class RefExpr(override val identifierToken: Token, val generics: GenericParams? = null) : Expr(), Typed, Named {
    override val identifierWindow: Window
        get() = identifierToken.asWindow(allTokens.tokens)

    private var _type: ClassType? = null

    /**
     * Type of reference destination
     */
    override val type: ClassType
        get() {
            return _type!!
        }

    override val isTypeKnown: Boolean
        get() = _type != null

    override val rootType: ClassType
        get() = type

    override val isRootTypeKnown: Boolean
        get() = isTypeKnown

    val isMethod: Boolean
        get() = ref is MethodDecl
    val isConstructor: Boolean
        get() = ref is ConstructorDecl || ref is ClassDecl
    val isField: Boolean
        get() = ref is FieldDecl
    val isVar: Boolean
        get() = ref is VarDecl
    val isParam: Boolean
        get() = ref is Param

    private var _ref: Decl? = null
    val ref: Decl?
        get() = _ref

    fun specifyRef(decl: Decl) {
        if (_ref != null) require(
            (decl is MethodDecl && _ref is MethodDecl && decl.identifier == _ref!!.identifier)
                || (decl is ConstructorDecl && _ref is ClassDecl && decl.outerDecl.identifier == _ref!!.identifier)
        )
        _ref = decl
    }

    override val window: Window
        get() = Window(identifierToken, generics?.tokens?.last() ?: identifierToken, allTokens.tokens)

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

    override val window: Window
        get() = Window(token, token, allTokens.tokens)

    private var _type: ClassType? = null

    override val type: ClassType
        get() {
            return _type!!
        }

    override val isTypeKnown: Boolean
        get() = _type != null

    override val isRootTypeKnown: Boolean
        get() = isTypeKnown

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

    override val window: Window
        get() = Window(token, token, allTokens.tokens)

    private var _type: ClassType? = null

    override val type: ClassType
        get() {
            return _type!!
        }

    override val isTypeKnown: Boolean
        get() = _type != null

    override val isRootTypeKnown: Boolean
        get() = isTypeKnown

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

    override val window: Window
        get() = Window(token, token, allTokens.tokens)

    private var _type: ClassType? = null

    override val type: ClassType
        get() {
            return _type!!
        }

    override val isTypeKnown: Boolean
        get() = _type != null

    override val rootType: ClassType
        get() = type

    override val isRootTypeKnown: Boolean
        get() = isTypeKnown

    override fun specifyType(type: Type) {
        require(type is ClassType)
        _type = type
    }

    override fun <T> visit(visitor: Visitor<T>): T? = walk(visitor, visitor.order, visitor.onBroken)
}

class CallExpr(val ref: RefExpr, val lparen: Token, val rparen: Token) : Expr() {
    private val _args = ArrayList<Argument>()

    val args: List<Argument> get() = _args

    val argumentTypes: List<ClassType>
        get() = args.map { it.rootType }

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

    override val window: Window
        get() = Window(ref.tokens.first(), rparen, allTokens.tokens)

    private var _type: ClassType? = null

    override val type: ClassType
        get() {
            return _type!!
        }

    override val isTypeKnown: Boolean
        get() = _type != null

    override val isRootTypeKnown: Boolean
        get() = isTypeKnown

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
    val left: Expr,
    val dot: Token,
    val member: Expr
) : Expr() {
    init {
        require(member is CallExpr || member is RefExpr || member is MemberAccessExpr)
    }

    val isCall: Boolean get() = member is CallExpr || (member is MemberAccessExpr && member.isCall)

    override val window: Window
        get() = Window(left.tokens.first(), member.tokens.last(), allTokens.tokens)

    private var _type: ClassType? = null

    override val type: ClassType
        get() {
            return _type!!
        }

    override val isTypeKnown: Boolean
        get() = _type != null

    override val isRootTypeKnown: Boolean
        get() = isTypeKnown

    override val rootType: ClassType
        get() = type

    override fun specifyType(type: Type) {
        require(type is ClassType)
        _type = type
    }

    override fun <T> visit(visitor: Visitor<T>): T? = walk(visitor, visitor.order, visitor.onBroken, left, member)
}