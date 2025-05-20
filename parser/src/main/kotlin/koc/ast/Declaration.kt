package koc.ast

import koc.lex.Token
import koc.lex.Window
import koc.parser.ast.Identifier
import koc.ast.visitor.Visitor
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
}

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

    val hasExplicitSuperType: Boolean get() = superTypeRef != null

    private var _type: ClassType? = null

    override val type: ClassType
        get() {
            ensureAfterSema()
            return _type!!
        }

    override val rootType: ClassType
        get() = type

    val superType: ClassType?
        get() {
            return type.superType
        }

    override fun <T> visit(visitor: Visitor<T>): T? = walk(
        visitor, visitor.order, visitor.onBroken, *listOfNotNull(
            generics, superTypeRef, body
        ).toTypedArray()
    )

    override val window: Window
        get() = Window(classToken, body.tokens.last(), allTokens)

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

    override val rootType: ClassType
        get() = type.classType

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

    override fun <T> visit(visitor: Visitor<T>): T? = walk(
        visitor, visitor.order, visitor.onBroken, *listOfNotNull(params, body).toTypedArray()
    )

    override val window: Window
        get() = Window(thisToken, body.tokens.last(), allTokens)
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

    override fun <T> visit(visitor: Visitor<T>): T? = walk(
        visitor, visitor.order, visitor.onBroken, *listOfNotNull(params, retTypeRef, body).toTypedArray()
    )

    override val window: Window
        get() = Window(keyword, body?.tokens?.last() ?: retTypeRef?.tokens?.last() ?: colon ?: params?.tokens?.last() ?: identifierToken, allTokens)
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

    override val rootType: ClassType
        get() = type.classType

    override fun specifyType(type: Type) {
        require(type is VarType)
        _type = type
    }

    override fun <T> visit(visitor: Visitor<T>): T? = walk(visitor, visitor.order, visitor.onBroken, initializer)

    val identifier: Identifier
        get() = Identifier(identifierToken.value)

    override val window: Window
        get() = Window(keyword, initializer.tokens.last(), allTokens)

    override fun toString(indent: Int, builder: StringBuilder) {
        builder.append(indent.indent, "File(${identifier})").scope {
            append(indent.indent, "var: $keyword").appendLine()
            append(indent.indent, "identifier: $identifierToken").appendLine()
            append(indent.indent, "colon: $colonToken").appendLine()

            initializer.toString(indent.next, builder)
        }
    }
}

data class Param(
    val identifierToken: Token,
    val colonToken: Token,
    val typeRef: RefExpr,
    /**
     * comma before the param
     */
    val commaToken: Token? = null
) : Node(), Typed {
    val identifier: Identifier = Identifier(identifierToken.value)

    private var _type: ParamType? = null
    override val type: ParamType
        get() {
            ensureAfterSema()
            return _type!!
        }

    override fun specifyType(type: Type) {
        require(type is ParamType)
        _type = type
    }

    override fun <T> visit(visitor: Visitor<T>): T? = walk(visitor, visitor.order, visitor.onBroken, typeRef)

    override val rootType: ClassType
        get() = type.classType

    override val window: Window
        get() = Window(identifierToken, commaToken ?: typeRef.tokens.last(), allTokens)
}

data class TypeParam(
    val typeRef: RefExpr,
    val commaToken: Token? = null
) : Node() {
    override fun <T> visit(visitor: Visitor<T>): T? = walk(visitor, visitor.order, visitor.onBroken, typeRef)

    override val window: Window
        get() = Window(typeRef.tokens.first(), commaToken ?: typeRef.tokens.last(), allTokens)
}
