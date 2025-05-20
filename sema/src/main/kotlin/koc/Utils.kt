package koc

import koc.ast.ClassType

fun ClassType.isSubtype(of: ClassType): Boolean {
    var child: ClassType? = this
    while (child != null) {
        if (child.identifier == of.identifier) return true
        child = child.classDecl.superType
    }
    return false
}

fun ClassType.isSuperType(of: ClassType): Boolean = of.isSubtype(this)
