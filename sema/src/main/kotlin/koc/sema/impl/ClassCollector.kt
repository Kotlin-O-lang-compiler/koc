package koc.sema.impl

import koc.core.Diagnostics
import koc.lex.diag
import koc.parser.ast.Attribute
import koc.ast.ClassDecl
import koc.ast.visitor.AbstractVoidVisitor
import koc.ast.visitor.Insight
import koc.sema.TypeManager
import koc.sema.diag.BuiltInClassRedefinition
import koc.sema.diag.ClassRedefinition

class ClassCollector(
    private val typeManager: TypeManager, private val diag: Diagnostics
) : AbstractVoidVisitor() {
    override fun visit(classDecl: ClassDecl) {
        val name = classDecl.identifier.value
        if (name in TypeManager.builtinTypes) {
            diag.diag(BuiltInClassRedefinition(classDecl), classDecl.identifierToken)
            classDecl.enable(Attribute.BROKEN)
        } else {
            if (typeManager.hasUserDefinition(name)) {
                diag.diag(ClassRedefinition(classDecl, typeManager.getUserDefinition(name)), classDecl.identifierToken)
                classDecl.enable(Attribute.BROKEN)
            }
        }

        typeManager.learn(classDecl)
        emit(Insight.SKIP)
    }
}