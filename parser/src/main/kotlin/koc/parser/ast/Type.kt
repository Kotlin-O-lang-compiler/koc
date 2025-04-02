package koc.parser.ast

import java.util.EnumSet

interface Typed {
    val type: Type

    fun specifyType(type: Type)
}

sealed class Type() : Attributed {
    init {
        enable(Attribute.AFTER_TYPE_CHECK)
    }

    private val _attrs: MutableSet<Attribute> = EnumSet.noneOf(Attribute::class.java)

    final override val attrs: Set<Attribute> = _attrs

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
class ClassType(val classDecl: ClassDecl, val superType: ClassType?) : Type() {
}


class VarType(val varDecl: VarDecl, val classType: ClassType) : Type() {

}

sealed class ClassMemberType(val outerDecl: ClassDecl) : Type()

class FieldType(val varDecl: VarDecl, val classType: ClassType, outerDecl: ClassDecl) : ClassMemberType(outerDecl) {

}