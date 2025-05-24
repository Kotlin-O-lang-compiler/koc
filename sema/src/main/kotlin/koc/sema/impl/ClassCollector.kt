package koc.sema.impl

import koc.ast.ClassDecl
import koc.ast.visitor.AbstractVoidInsightVisitor
import koc.ast.visitor.Insight
import koc.core.Diagnostics
import koc.lex.diag
import koc.parser.ast.Attribute
import koc.sema.TypeManager
import koc.sema.diag.BuiltInClassRedefinition
import koc.sema.diag.DeclRedefinition

class ClassCollector(
    private val typeManager: TypeManager, private val diag: Diagnostics, private val scopeManager: ScopeManager
) : AbstractVoidInsightVisitor() {
    override fun visit(classDecl: ClassDecl): Insight {
        val name = classDecl.identifier.value
        if (name in TypeManager.builtinTypes) {
            diag.diag(BuiltInClassRedefinition(classDecl), classDecl.identifierToken)
            classDecl.enable(Attribute.BROKEN)
        } else {
            if (typeManager.hasUserDefinition(name)) {
                diag.diag(DeclRedefinition(classDecl, typeManager.getUserDefinition(name)), classDecl.identifierToken)
                classDecl.enable(Attribute.BROKEN)
            }
        }

        typeManager.learn(classDecl)
        scopeManager += classDecl
        return Insight.SKIP
    }
}