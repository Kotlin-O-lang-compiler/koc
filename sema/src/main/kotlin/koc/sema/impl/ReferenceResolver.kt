package koc.sema.impl

import koc.ast.ClassDecl
import koc.ast.FieldDecl
import koc.ast.MethodDecl
import koc.ast.Param
import koc.ast.RefExpr
import koc.ast.TypeParam
import koc.ast.VarDecl
import koc.ast.visitor.AbstractVoidInsightVisitor
import koc.ast.visitor.Insight
import koc.core.Diagnostics
import koc.lex.diag
import koc.parser.ast.Attribute
import koc.sema.TypeManager
import koc.sema.diag.ThisOutOfContext
import koc.sema.diag.UndefinedReference

class ReferenceResolver(
    private val typeManager: TypeManager,
    private val diag: Diagnostics,
    private val scopeManager: ScopeManager
) : AbstractVoidInsightVisitor() {
    private var context: ClassDecl? = null

    override fun visit(field: FieldDecl): Insight {
        scopeManager += field
        field.varDecl.initializer.visit(this)
        return Insight.SKIP
    }

    override fun visit(vardecl: VarDecl): Insight {
        scopeManager += vardecl
        return super.visit(vardecl)
    }

    override fun visit(method: MethodDecl): Insight {
        scopeManager += method
        return super.visit(method)
    }

    override fun visit(param: Param): Insight {
        scopeManager += param
        return super.visit(param)
    }

    override fun visit(node: TypeParam): Insight {
        scopeManager += node
        return super.visit(node)
    }

    override fun visit(expr: RefExpr): Insight {
        if (expr.ref != null) return Insight.SKIP

        if (expr.isThis) {
            if (context != null) {
                expr.specifyRef(context!!)
            } else {
                diag.diag(ThisOutOfContext(expr), expr.window)
                expr.specifyRef(typeManager.invalidDecl)
                expr.enable(Attribute.BROKEN)
            }
        } else {
            if (scopeManager.isDefined(expr.identifier, expr.scope)) {
                expr.specifyRef(scopeManager.getDecl(expr.identifier, expr.scope))
            } else {
                expr.specifyRef(typeManager.invalidDecl)
                diag.diag(UndefinedReference(expr), expr.window)
                expr.enable(Attribute.BROKEN)
            }
        }
        return Insight.CONTINUE
    }

    override fun previsit(classDecl: ClassDecl) {
        context = classDecl
    }

    override fun postvisit(classDecl: ClassDecl, res: Insight) {
        context = null
    }
}