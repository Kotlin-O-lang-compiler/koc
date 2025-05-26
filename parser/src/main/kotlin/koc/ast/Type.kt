package koc.ast

import koc.parser.ast.Attribute
import koc.parser.ast.Attributed
import koc.parser.ast.Identifier
import java.util.EnumSet

interface Typed {
    val type: Type
    val rootType: ClassType
        get() = throw IllegalStateException("Type does not have class type")

    val isTypeKnown: Boolean

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
data class ClassType(val classDecl: ClassDecl, val superType: ClassType?) : Type() {
    init {
        require(superType != null || classDecl.identifier.value == "Class"
                || classDecl.identifier.value == "\$invalid" || classDecl.identifier.value == "\$Unit"
        ) {
            "Only root `Class` declaration is allowed to not have super type"
        }
    }

    val identifier: Identifier
        get() = classDecl.identifier

    val isUnit: Boolean
        get() = identifier.value == "\$Unit"

    val isGeneric: Boolean
        get() = classDecl.generics != null && classDecl.generics.types.isNotEmpty()
}

sealed class RefType(
    val isMethod: Boolean = false,
    val isConstructor: Boolean = false,
    val isClass: Boolean = false,
    val isEntity: Boolean = false
): Type() {
    init {
        require(isMethod || isConstructor || isClass || isEntity)
        var active = arrayOf(isMethod, isConstructor, isClass, isEntity).count { active -> active }
        require(active == 1) { "Only one active reference kind is allowed" }
    }

    abstract val identifier: Identifier
    open val rootType: ClassType
        get() = throw IllegalStateException("Reference ${this::class.simpleName} has not root type")
}

data class ConstructorRefType(val ctor: ConstructorDecl) : RefType(isConstructor = true) {
    override val identifier: Identifier
        get() = ctor.identifier
}

data class MethodRefType(val method: MethodDecl): RefType(isMethod = true) {
    override val identifier: Identifier
        get() = method.identifier

    val type: MethodType
        get() = method.type
}

/**
 * Reference on class declaration (as a type)
 */
data class ClassRefType(val classDecl: ClassDecl): RefType(isClass = true) {
    override val identifier: Identifier
        get() = classDecl.identifier

    override val rootType: ClassType
        get() = classDecl.type
}

/**
 * Any entity reference such as: variable, field, parameter, `this` (for `ClassDecl`)
 */
data class EntityRefType(val ref: Decl) : RefType(isEntity = true) {
    init {
        require(ref is VarDecl || ref is FieldDecl || ref is Param || ref is ClassDecl)
    }
    override val identifier: Identifier
        get() = ref.identifier

    override val rootType: ClassType
        get() = ref.rootType

    val isParam: Boolean
        get() = ref is Param
    val isField: Boolean
        get() = ref is FieldDecl
    val isVar: Boolean
        get() = ref is VarDecl
    val isThis: Boolean
        get() = ref is ClassDecl
}

data class VarType(val varDecl: VarDecl, val classType: ClassType) : Type() {
}

sealed class ClassMemberType(val outerDecl: ClassDecl) : Type()

class FieldType(val field: FieldDecl, outerDecl: ClassDecl) : ClassMemberType(outerDecl) {
    val classType: ClassType
        get() = this.field.rootType

}

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