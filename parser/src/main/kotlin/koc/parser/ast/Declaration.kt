package koc.parser.ast

import koc.lex.Token
import koc.utils.Position

sealed class Decl() : Node(), Typed

class ClassDecl(
    val classToken: Token,
    val identifierToken: Token,
    val extendsToken: Token? = null,
    val superTypeRef: ClassReference? = null,
    val body: ClassBody,
) : Decl() {

    init {
        require(extendsToken != null && superTypeRef != null || extendsToken == null && superTypeRef == null) {
            "Both `extendsToken` and `superTypeRef` could be non-null simultaneously"
        }
    }

    val identifier: Identifier get() = Identifier(identifierToken.value)

    val hasExplicitSuperType: Boolean get() = extendsToken != null

    private var _type: ClassType? = null

    override val type: ClassType
        get() {
            ensureAfterSema()
            return _type!!
        }

    val superType: ClassType?
        get() {
            return type.superType
        }

    override val tokens: List<Token>
        get() {
            val tokens = arrayListOf<Token>()
            tokens += classToken
            tokens += identifierToken
            if (hasExplicitSuperType) {
                tokens += extendsToken!!
                tokens += superTypeRef!!.tokens
                tokens += body.tokens
            }
            return tokens
        }

    override fun specifyType(type: Type) {
        require(type is ClassType)
        _type = type
    }
}

sealed class ClassMemberDecl() : Decl() {
    abstract val outerDecl: ClassDecl
}

class FieldDecl(
    val varDecl: VarDecl,
    override val outerDecl: ClassDecl
) : ClassMemberDecl() {

    private var _type: FieldType? = null

    override val type: FieldType
        get() {
            ensureAfterSema()
            return _type!!
        }

    override fun specifyType(type: Type) {
        require(type is FieldType)
        _type = type
    }

    override val tokens: List<Token>
        get() = varDecl.tokens
}

class ConstructorDecl(
    val thisToken: Token,
    val params: Params? = null,
    val isToken: Token,
    val body: Body,
    val endToken: Token
) : Node() {

    override val tokens: List<Token> get() {
        val res = ArrayList<Token>()
        res += thisToken
        params?.let { res += it.tokens }
        res += isToken
        res += body.tokens
        res += endToken
        return res
    }
}

class MethodDecl(
    val keyword: Token,
    val identifierToken: Token,
    val params: Params? = null,
    val retTypeRef: ClassReference? = null,
    val body: MethodBody? = null
) : Node() {

    override val tokens: List<Token>
        get() {
            val res = ArrayList<Token>()
            res += keyword
            res += identifierToken
            params?.let { res += it.tokens }
            retTypeRef?.let { res += it.tokens }
            body?.let { res += it.tokens }
            return res
        }
}

class VarDecl(
    val keyword: Token,
    val identifierToken: Token,
    val colonToken: Token,
    val initializer: Expr
) : Decl() {

    private var _type: VarType? = null

    override val type: VarType
        get() {
            ensureAfterSema()
            return _type!!
        }

    override fun specifyType(type: Type) {
        require(type is VarType)
        _type = type
    }

    override val tokens: List<Token>
        get() = listOf(keyword, identifierToken, colonToken, *initializer.tokens.toTypedArray())
}

class Param(
    val identifierToken: Token,
    val colonToken: Token,
    val typeRef: ClassReference,
    val commaToken: Token? = null
) : Node() {
    val identifier: Identifier = Identifier(identifierToken.value)

    override val tokens: List<Token>
        get() {
            val res = arrayListOf(identifierToken, colonToken)
            res += typeRef.tokens
            commaToken?.let { res += it }
            return res
        }
}
