package koc.sema.impl

import koc.ast.ClassDecl
import koc.ast.MethodDecl
import koc.ast.ReturnNode
import koc.ast.visitor.AbstractVoidInsightVisitor
import koc.ast.visitor.Insight
import koc.core.Diagnostics
import koc.lex.diag
import koc.parser.ast.Attribute
import koc.sema.TypeManager
import koc.sema.diag.NonReturningCallInExpr
import koc.sema.diag.TypeMismatch

class TypeChecker(
    private val typeManager: TypeManager,
    private val diag: Diagnostics,
    private val scopeManager: ScopeManager,
    private val overloadManager: OverloadManager
) : AbstractVoidInsightVisitor() {
    private var context: MethodDecl? = null

    override fun visit(node: ReturnNode): Insight {
        if (node.expr == null) {
            if (context!!.type.retType?.isUnit == false) {
                diag.diag(TypeMismatch(context!!.type.retType!!, typeManager.unitType, node.allCode), node.window)
                node.enable(Attribute.BROKEN)
                context!!.enable(Attribute.BROKEN)
                return Insight.SKIP
            }
        } else {
            val ret = node.expr!!.rootType
            if (ret.isUnit) {
                diag.diag(NonReturningCallInExpr(node.expr!!), node.expr!!.window)
                node.enable(Attribute.BROKEN)
                context!!.enable(Attribute.BROKEN)
                return Insight.SKIP
            }

            if (ret.identifier != context!!.type.retType!!.identifier) {
                diag.diag(TypeMismatch(context!!.type.retType!!, ret, node.allCode), node.expr!!.window)
                node.enable(Attribute.BROKEN)
                context!!.enable(Attribute.BROKEN)
            }
        }
        return Insight.SKIP
    }

    override fun previsit(methodDecl: MethodDecl) {
        context = methodDecl
    }

    override fun postvisit(methodDecl: MethodDecl, res: Insight) {
        context = null
    }

}