package koc.ast

import koc.lex.Token
import koc.lex.Window
import koc.parser.ast.Identifier
import koc.ast.visitor.Visitor
import koc.lex.asWindow
import koc.parser.indent
import koc.parser.next
import koc.parser.scope
import koc.parser.walk

sealed class Decl() : Node(), Typed {
    override fun toString(indent: Int, builder: StringBuilder) {
        builder.append(indent.indent, this.javaClass.simpleName).scope {
            builder.appendLine(tokens.toString().prependIndent(indent.indent))
        }
    }

    abstract val identifierToken: Token
    val identifier: Identifier
        get() = Identifier(identifierToken.value)

    val identifierWindow: Window
        get() = identifierToken.asWindow(allTokens.tokens)

    abstract val declKindValue: String
}

data class ClassDecl(
    val classToken: Token,
    override val identifierToken: Token,
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

    val ref: RefExpr get() = RefExpr(identifierToken, generics)

    val hasExplicitSuperType: Boolean get() = superTypeRef != null

    private var _type: ClassType? = null

    override val type: ClassType
        get() {
            return _type!!
        }

    override val isTypeKnown: Boolean
        get() = _type != null

    override val rootType: ClassType
        get() = type

    val superType: ClassType?
        get() {
            return type.superType
        }

    val decls: List<ClassMemberDecl>
        get() = body.members

    val methods: List<MethodDecl>
        get() = decls.filterIsInstance<MethodDecl>()

    val fields: List<FieldDecl>
        get() = decls.filterIsInstance<FieldDecl>()

    val constructors: List<ConstructorDecl>
        get() = decls.filterIsInstance<ConstructorDecl>()

    override fun <T> visit(visitor: Visitor<T>): T? = walk(
        visitor, visitor.order, visitor.onBroken, *listOfNotNull(
            generics, superTypeRef, body
        ).toTypedArray()
    )

    override val window: Window
        get() = Window(classToken, body.tokens.last(), allTokens.tokens)

    override fun specifyType(type: Type) {
        require(type is ClassType)
        _type = type
    }

    override fun toString(indent: Int, builder: StringBuilder) {
        builder.append(indent.indent, "ClassDecl(${identifier})").scope {
            builder.append(indent.next, "class: $classToken").appendLine()
            builder.append(indent.next, "identifier: $identifierToken").appendLine()
            extendsToken?.let { builder.append(indent.next, "extends: $it").appendLine() }

            generics?.toString(indent.next, builder)
            superTypeRef?.toString(indent.next, builder)
            body.toString(indent.next, builder)
        }
    }

    override val declKindValue: String
        get() = "class"
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
    override val identifierToken: Token
        get() = varDecl.identifierToken

    private var _type: FieldType? = null

    override val type: FieldType
        get() {
            return _type!!
        }

    override val isTypeKnown: Boolean
        get() = _type != null

    override val rootType: ClassType
        get() = varDecl.rootType

    override fun specifyType(type: Type) {
        require(type is FieldType)
        _type = type
    }

    override fun <T> visit(visitor: Visitor<T>): T? = walk(visitor, visitor.order, visitor.onBroken, varDecl)

    override val window: Window
        get() = varDecl.window

    override fun toString(indent: Int, builder: StringBuilder) {
        varDecl.toString(indent, builder)
    }

    override val declKindValue: String
        get() = "field"
}

data class ConstructorDecl(
    val thisToken: Token,
    val params: Params? = null,
    val body: Body,
) : ClassMemberDecl() {
    override val identifierToken: Token
        get() = thisToken

    private var _type: ConstructorType? = null

    override val type: ConstructorType
        get() {
            ensureAfterSema()
            return _type!!
        }

    override val isTypeKnown: Boolean
        get() = _type != null

    override fun specifyType(type: Type) {
        require(type is ConstructorType)
        _type = type
    }

    override fun <T> visit(visitor: Visitor<T>): T? = walk(
        visitor, visitor.order, visitor.onBroken, *listOfNotNull(params, body).toTypedArray()
    )

    val signatureWindow: Window
        get() = Window(thisToken, params?.window?.endToken ?: identifierToken, allTokens.tokens)

    override val window: Window
        get() = Window(thisToken, body.tokens.last(), allTokens.tokens)

    override val declKindValue: String
        get() = "constructor"
}

data class MethodDecl(
    val keyword: Token,
    override val identifierToken: Token,
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
            return _type!!
        }

    override val isTypeKnown: Boolean
        get() = _type != null

    override fun specifyType(type: Type) {
        require(type is MethodType)
        _type = type
    }

    override fun <T> visit(visitor: Visitor<T>): T? = walk(
        visitor, visitor.order, visitor.onBroken, *listOfNotNull(params, retTypeRef, body).toTypedArray()
    )

    override val window: Window
        get() = Window(keyword, body?.tokens?.last() ?: retTypeRef?.tokens?.last() ?: colon ?: params?.tokens?.last() ?: identifierToken, allTokens.tokens)

    val signatureWindow: Window
        get() = Window(keyword, retTypeRef?.window?.endToken ?: params?.window?.endToken ?: identifierToken, allTokens.tokens)

    override val declKindValue: String
        get() = "method"
}

data class VarDecl(
    val keyword: Token,
    override val identifierToken: Token = Token.invalid,
    val colonToken: Token = Token.invalid,
    val initializer: Expr
) : Decl() {
    private var _type: VarType? = null

    override val type: VarType
        get() {
            return _type!!
        }

    override val isTypeKnown: Boolean
        get() = _type != null

    override val rootType: ClassType
        get() = type.classType

    override fun specifyType(type: Type) {
        require(type is VarType)
        _type = type
    }

    override fun <T> visit(visitor: Visitor<T>): T? = walk(visitor, visitor.order, visitor.onBroken, initializer)

    override val window: Window
        get() = Window(keyword, initializer.tokens.last(), allTokens.tokens)

    override fun toString(indent: Int, builder: StringBuilder) {
        builder.append(indent.indent, "File(${identifier})").scope {
            append(indent.indent, "var: $keyword").appendLine()
            append(indent.indent, "identifier: $identifierToken").appendLine()
            append(indent.indent, "colon: $colonToken").appendLine()

            initializer.toString(indent.next, builder)
        }
    }

    override val declKindValue: String
        get() = "variable"
}

data class Param(
    override val identifierToken: Token,
    val colonToken: Token,
    val typeRef: RefExpr,
    /**
     * comma before the param
     */
    val commaToken: Token? = null
) : Decl(), Typed {
    private var _type: ParamType? = null
    override val type: ParamType
        get() {
            return _type!!
        }

    override val isTypeKnown: Boolean
        get() = _type != null

    override fun specifyType(type: Type) {
        require(type is ParamType)
        _type = type
    }

    override fun <T> visit(visitor: Visitor<T>): T? = walk(visitor, visitor.order, visitor.onBroken, typeRef)

    override val rootType: ClassType
        get() = type.classType

    override val window: Window
        get() = Window(identifierToken, commaToken ?: typeRef.tokens.last(), allTokens.tokens)

    override val declKindValue: String
        get() = "parameter"
}

data class TypeParam(
    val typeRef: RefExpr,
    val commaToken: Token? = null
) : Decl() {
    override val identifierToken: Token
        get() = typeRef.identifierToken

    override fun <T> visit(visitor: Visitor<T>): T? = walk(visitor, visitor.order, visitor.onBroken, typeRef)

    private var _type: TypeParamType? = null
    override val type: TypeParamType
        get() = _type!!

    override val isTypeKnown: Boolean
        get() = _type != null

    override fun specifyType(type: Type) {
        require(type is TypeParamType)
        _type = type
    }

    override val window: Window
        get() = Window(typeRef.tokens.first(), commaToken ?: typeRef.tokens.last(), allTokens.tokens)

    override val declKindValue: String
        get() = "type parameter"
}
