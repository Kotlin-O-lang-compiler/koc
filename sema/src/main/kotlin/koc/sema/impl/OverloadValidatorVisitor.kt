package koc.sema.impl

import koc.ast.ClassDecl
import koc.ast.ClassRefType
import koc.ast.ConstructorDecl
import koc.ast.ConstructorType
import koc.ast.MethodDecl
import koc.ast.MethodType
import koc.ast.Param
import koc.ast.ParamType
import koc.ast.Params
import koc.ast.RefExpr
import koc.ast.visitor.AbstractVoidInsightVisitor
import koc.ast.visitor.Insight
import koc.core.Diagnostics
import koc.lex.diag
import koc.parser.ast.Attribute
import koc.sema.TypeManager
import koc.sema.diag.UndefinedReference

class OverloadValidatorVisitor(
    private val typeManager: TypeManager,
    private val diag: Diagnostics,
    private val scopeManager: ScopeManager,
    private val overloadManager: OverloadManager,
) : AbstractVoidInsightVisitor() {
    override fun postvisit(params: Params, res: Insight) {
        if (params.params.any { it.isBroken }) {
            params.enable(Attribute.BROKEN)
            return
        }
        require(params.params.map { it.identifier }.toHashSet().size == params.params.size)
    }

    override fun visit(param: Param): Insight {
        scopeManager += param
        return super.visit(param)
    }

    override fun postvisit(param: Param, res: Insight) {
        visitParamRef(param.typeRef)
        if (!param.typeRef.isBroken && (!param.typeRef.isTypeKnown || !param.typeRef.type.isClass)) {
            diag.diag(UndefinedReference(param.typeRef), param.typeRef.window)
            param.enable(Attribute.BROKEN)
            param.specifyType(ParamType(param, typeManager.invalidType))
        } else {
            val typeRef = param.typeRef.type
            if (!param.typeRef.isBroken) param.specifyType(ParamType(param, typeRef.rootType))
            else param.specifyType(ParamType(param, typeManager.invalidType))
        }
    }

    private fun visitParamRef(expr: RefExpr) {
        if (scopeManager.isDefined(expr.identifier, expr.scope)) {
            val decl = scopeManager.getDecl(expr.identifier, expr.scope)
            expr.specifyRef(decl)
            val refType = if (decl is ClassDecl) ClassRefType(decl) else {
                diag.diag(UndefinedReference(expr), expr.window)
                expr.enable(Attribute.BROKEN)
                return
            }
            expr.specifyType(refType)
        } else {
            expr.specifyRef(typeManager.invalidDecl)
            expr.specifyType(typeManager.invalidType)
            diag.diag(UndefinedReference(expr), expr.window)
            expr.enable(Attribute.BROKEN)
        }
    }

    override fun visit(ctor: ConstructorDecl): Insight {
        ctor.params?.visit(this)
        overloadManager += ctor

        ctor.specifyType(ConstructorType(ctor, ctor.outerDecl))

        return Insight.SKIP
    }

    override fun visit(method: MethodDecl): Insight {
        method.params?.visit(this)
        method.retTypeRef?.also(::visitParamRef)
        overloadManager += method

        val retType = method.retTypeRef?.rootType
        val methodType = MethodType(method, method.outerDecl, retType)
        method.specifyType(methodType)

        return Insight.SKIP
    }
}