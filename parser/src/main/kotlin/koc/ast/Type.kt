package koc.ast

import koc.lex.Token
import koc.lex.Window
import koc.parser.ast.Attribute
import koc.parser.ast.Attributed
import koc.parser.ast.Identifier
import java.util.EnumSet

interface Named {
    val identifierToken: Token

    val identifier: Identifier
        get() = Identifier(identifierToken.value)

    val identifierWindow: Window
}

interface Typed {
    val type: Type
    val rootType: ClassType
        get() = throw IllegalStateException("Type does not have class type")

    val isTypeKnown: Boolean

    val isRootTypeKnown: Boolean

    fun specifyType(type: Type)
}

sealed class Type() : Attributed {
    private val _attrs: MutableSet<Attribute> = EnumSet.noneOf(Attribute::class.java)

    final override val attrs: Set<Attribute> = _attrs

    init {
        enable(Attribute.AFTER_TYPE_CHECK)
    }

    final override fun enable(attr: Attribute) {
        _attrs += attr
    }

    final override fun disable(attr: Attribute) {
        _attrs -= attr
    }
}

/**
 * @param superType: the root class is `Class` that does not have `superType`, other classes does.
 */
data class ClassType(val classDecl: ClassDecl, val superType: ClassType?) : Type(), Named {
    init {
        require(superType != null || classDecl.identifier.value == "Class"
                || classDecl.identifier.value == "\$invalid" || classDecl.identifier.value == "\$Unit"
        ) {
            "Only root `Class` declaration is allowed to not have super type"
        }
    }

    override val identifierToken: Token
        get() = classDecl.identifierToken

    override val identifierWindow: Window
        get() = classDecl.identifierWindow

    val isUnit: Boolean
        get() = identifier.value == "\$Unit"

    val isGeneric: Boolean
        get() = classDecl.generics != null && classDecl.generics.types.isNotEmpty()
}

sealed class ClassMemberType(val outerDecl: ClassDecl) : Type()

class ConstructorType(val ctor: ConstructorDecl, outerDecl: ClassDecl) : ClassMemberType(outerDecl) {
    val params: List<Param>
        get() = ctor.params?.params ?: emptyList()
}

class MethodType(val method: MethodDecl, outerDecl: ClassDecl, val retType: ClassType? = null) : ClassMemberType(outerDecl) {
    val params: List<Param>
        get() = method.params?.params ?: emptyList()

    val paramTypes: List<ClassType>
        get() = params.map { it.type.classType }

    val hasRetType: Boolean
        get() = retType != null

    val isVoid: Boolean
        get() = !hasRetType
}

data class ParamType(val param: Param, val classType: ClassType): Type()

data class TypeParamType(val param: TypeParam, val classType: ClassType? = null): Type() {
    val isGeneric: Boolean
        get() = classType == null

    val isConcrete: Boolean
        get() = !isGeneric
}