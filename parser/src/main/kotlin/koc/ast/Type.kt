package koc.parser.ast

import java.util.EnumSet

interface Typed {
    val type: Type
    val rootType: ClassType
        get() = throw IllegalStateException("Type does not have class type")

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
        require(superType != null || classDecl.identifier.value == "Class") {
            "Only root `Class` declaration is allowed to not have super type"
        }
    }

    val identifier: Identifier
        get() = classDecl.identifier

    val isGeneric: Boolean
        get() = classDecl.generics != null && classDecl.generics.types.isNotEmpty()
}


data class VarType(val varDecl: VarDecl, val classType: ClassType) : Type() {
}

sealed class ClassMemberType(val outerDecl: ClassDecl) : Type()

class FieldType(val field: FieldDecl, outerDecl: ClassDecl) : ClassMemberType(outerDecl) {
    val classType: ClassType
        get() = this.field.rootType

}

class ConstructorType(val ctor: ConstructorDecl, outerDecl: ClassDecl) : ClassMemberType(outerDecl) {

}

class MethodType(val method: MethodDecl, outerDecl: ClassDecl) : ClassMemberType(outerDecl) {

}

data class ParamType(val param: Param, val classType: ClassType): Type()