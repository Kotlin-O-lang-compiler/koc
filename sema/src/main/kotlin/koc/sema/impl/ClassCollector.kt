package koc.sema.impl

import koc.parser.ast.Attribute
import koc.parser.ast.ClassDecl
import koc.parser.ast.visitor.AbstractVoidVisitor
import koc.parser.ast.visitor.Insight
import koc.sema.TypeManager
import koc.sema.exception.BuiltInClassRedefinitionException
import koc.sema.exception.ClassRedefinitionException
import koc.utils.Diagnostics

class ClassCollector(
    private val typeManager: TypeManager, private val diag: Diagnostics
) : AbstractVoidVisitor() {
    override fun visit(classDecl: ClassDecl) {
        val name = classDecl.identifier.value
        if (name in TypeManager.builtinTypes) {
            diag.error(BuiltInClassRedefinitionException(classDecl), classDecl.identifierToken.start)
            classDecl.enable(Attribute.BROKEN)
        } else {
            if (typeManager.hasUserDefinition(name)) {
                diag.error(
                    ClassRedefinitionException(classDecl, typeManager.getUserDefinition(name)),
                    classDecl.identifierToken.start
                )
                classDecl.enable(Attribute.BROKEN)
            }
        }

        typeManager.learn(classDecl)
        emit(Insight.SKIP)
    }
}