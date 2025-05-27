package koc.sema.impl

import koc.ast.CallExpr
import koc.ast.ClassDecl
import koc.ast.MemberAccessExpr
import koc.ast.RefExpr
import koc.ast.VarDecl
import koc.ast.visitor.AbstractVoidInsightVisitor
import koc.ast.visitor.Insight
import koc.core.Diagnostics
import koc.lex.diag
import koc.parser.ast.Attribute
import koc.sema.TypeManager
import koc.sema.diag.UndefinedReference

class VariableAccessOrderChecker(
    private val typeManager: TypeManager,
    private val diag: Diagnostics,
    private val scopeManager: ScopeManager
) : AbstractVoidInsightVisitor() {
    private var context: ClassDecl? = null

    override fun postvisit(vardecl: VarDecl, res: Insight) {
        if (vardecl.initializer.isBroken) {
            vardecl.enable(Attribute.BROKEN)
        }
    }

    override fun visit(expr: MemberAccessExpr): Insight {
        expr.left.visit(this)

        if (expr.left.isThis) {
            val thisRef = expr.left as RefExpr
            when (val member = expr.member) {
                is RefExpr -> if (member.isThis) member.setUndefined().also {
                    thisRef.enable(Attribute.BROKEN)
                    return Insight.SKIP
                }
                is MemberAccessExpr -> if (member.left.isThis) (member.left as RefExpr).setUndefined().also {
                    thisRef.enable(Attribute.BROKEN)
                    return Insight.SKIP
                }
                else -> Unit
            }
            expr.member.visit(this)
        } else {
            expr.left.visit(this)
        }

        return Insight.SKIP
    }

    override fun visit(expr: CallExpr): Insight {
        return Insight.SKIP
    }

    override fun visit(expr: RefExpr): Insight {
        if (expr.isThis) {
            if (context != null) {
                expr.specifyRef(context!!)
                expr.specifyType(context!!.type)
            } else {
                expr.specifyRef(typeManager.invalidDecl)
                expr.setUndefined()
            }
            return Insight.SKIP
        }

        if (scopeManager.isDefined(expr.identifier, expr.scope)) {
            val decl = scopeManager.getDecl(expr.identifier, expr.scope)
            if (expr.start < decl.start) {
                expr.setUndefined()
            }
        } else {
            expr.setUndefined()
        }
        return super.visit(expr)
    }

    override fun previsit(classDecl: ClassDecl) {
        context = classDecl
    }

    override fun postvisit(classDecl: ClassDecl, res: Insight) {
        context = null
    }

    private fun RefExpr.setUndefined() {
        diag.diag(UndefinedReference(this), window)
        this.enable(Attribute.BROKEN)
        this.specifyType(typeManager.invalidType)
    }
}