package koc.sema.impl

import koc.ast.ConstructorDecl
import koc.ast.FieldDecl
import koc.ast.MethodDecl
import koc.ast.visitor.AbstractVoidInsightVisitor
import koc.ast.visitor.Insight

class ClassMemberReferenceCollector(
    private val scopeManager: ScopeManager,
) : AbstractVoidInsightVisitor() {
    override fun visit(field: FieldDecl): Insight {
        scopeManager += field
        return Insight.SKIP
    }

    override fun visit(method: MethodDecl): Insight {
        scopeManager += method
        return Insight.SKIP
    }

    override fun visit(ctor: ConstructorDecl): Insight {
        return Insight.SKIP
    }
}