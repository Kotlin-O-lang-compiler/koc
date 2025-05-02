package koc.parser.ast

import koc.lex.Token

sealed class Decl() : Node(), Typed

data class ClassDecl(
    val classToken: Token,
    val identifierToken: Token,
    val generics: GenericParams? = null,
    val extendsToken: Token? = null,
    val superTypeRef: RefExpr? = null,
    val body: ClassBody,
) : Decl() {

    init {
        require(extendsToken != null && superTypeRef != null || extendsToken == null && superTypeRef == null) {
            "Both `extendsToken` and `superTypeRef` could be non-null simultaneously"
        }

        for (member in body.members) {
            if (!member.hasOuterDecl) member.initOuterDecl(this)
        }
    }

    val identifier: Identifier get() = Identifier(identifierToken.value)

    val ref: RefExpr get() = RefExpr(identifierToken, generics)

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
    open val outerDecl: ClassDecl
        get() = _outerDecl ?: throw IllegalStateException("Class declaration was not set")

    val hasOuterDecl: Boolean
        get() = _outerDecl != null

    private var _outerDecl: ClassDecl? = null

    fun initOuterDecl(outerDecl: ClassDecl) {
        _outerDecl = outerDecl
    }
}

data class FieldDecl(
    val varDecl: VarDecl
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

data class ConstructorDecl(
    val thisToken: Token,
    val params: Params? = null,
    val body: Body,
) : ClassMemberDecl() {
    private var _type: ConstructorType? = null

    override val type: ConstructorType
        get() {
            ensureAfterSema()
            return _type!!
        }

    override fun specifyType(type: Type) {
        require(type is ConstructorType)
        _type = type
    }

    override val tokens: List<Token> get() {
        val res = ArrayList<Token>()
        res += thisToken
        params?.let { res += it.tokens }
        res += body.tokens
        return res
    }
}

data class MethodDecl(
    val keyword: Token,
    val identifierToken: Token,
    val params: Params? = null,
    val colon: Token? = null,
    val retTypeRef: RefExpr? = null,
    val body: MethodBody? = null
) : ClassMemberDecl() {
    private var _type: MethodType? = null

    val isForwardDecl: Boolean
        get() = body == null

    override val type: MethodType
        get() {
            ensureAfterSema()
            return _type!!
        }

    override fun specifyType(type: Type) {
        require(type is MethodType)
        _type = type
    }

    override val tokens: List<Token>
        get() {
            val res = ArrayList<Token>()
            res += keyword
            res += identifierToken
            params?.let { res += it.tokens }
            colon?.let { res += it }
            retTypeRef?.let { res += it.tokens }
            body?.let { res += it.tokens }
            return res
        }
}

data class VarDecl(
    val keyword: Token,
    val identifierToken: Token = Token.invalid,
    val colonToken: Token = Token.invalid,
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

data class Param(
    val identifierToken: Token,
    val colonToken: Token,
    val typeRef: RefExpr,
    /**
     * comma before the param
     */
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

data class TypeParam(
    val typeRef: RefExpr,
    val commaToken: Token? = null
) : Node() {
    override val tokens: List<Token>
        get() {
            val res = arrayListOf<Token>()
            res += typeRef.tokens
            commaToken?.let { res += it }
            return res
        }
}