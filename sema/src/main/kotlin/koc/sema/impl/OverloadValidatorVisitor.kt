package koc.sema.impl

import koc.ast.ConstructorDecl
import koc.ast.MethodDecl
import koc.ast.visitor.AbstractVoidInsightVisitor
import koc.ast.visitor.Insight
import koc.core.Diagnostics
import koc.sema.TypeManager

class OverloadValidatorVisitor(
    private val typeManager: TypeManager, private val diag: Diagnostics, private val overloadManager: OverloadManager
) : AbstractVoidInsightVisitor() {
    override fun visit(ctor: ConstructorDecl): Insight {
        overloadManager += ctor
        return Insight.SKIP
    }

    override fun visit(method: MethodDecl): Insight {
        overloadManager += method
        return Insight.SKIP
    }
}