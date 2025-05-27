package koc.sema.impl

import koc.ast.ClassDecl
import koc.ast.ConstructorDecl
import koc.ast.ConstructorType
import koc.ast.MethodDecl
import koc.ast.MethodType
import koc.ast.Param
import koc.ast.ParamType
import koc.ast.Params
import koc.ast.TypeRef
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
        param.typeRef.visit(this)
        scopeManager += param
        return Insight.SKIP
    }

    override fun postvisit(param: Param, res: Insight) {
        if (!param.typeRef.isBroken && !param.typeRef.isTypeKnown) {
            diag.diag(UndefinedReference(param.typeRef), param.typeRef.window)
            param.enable(Attribute.BROKEN)
            param.specifyType(ParamType(param, typeManager.invalidType))
        } else {
            if (!param.typeRef.isBroken) param.specifyType(ParamType(param, param.typeRef.type))
            else param.specifyType(ParamType(param, typeManager.invalidType))
        }
    }

    override fun visit(ref: TypeRef): Insight {
        if (scopeManager.isDefined(ref.identifier, ref.scope)) {
            val decl = scopeManager.getDecl(ref.identifier, ref.scope)
            ref.specifyRef(decl)
            val refType = if (decl is ClassDecl) decl.type else {
                diag.diag(UndefinedReference(ref), ref.window)
                ref.enable(Attribute.BROKEN)
                typeManager.invalidType
            }
            ref.specifyType(refType)
        } else {
            ref.specifyRef(typeManager.invalidDecl)
            ref.specifyType(typeManager.invalidType)
            diag.diag(UndefinedReference(ref), ref.window)
            ref.enable(Attribute.BROKEN)
        }

        return super.visit(ref)
    }

    override fun visit(ctor: ConstructorDecl): Insight {
        ctor.params?.visit(this)
        overloadManager += ctor

        ctor.specifyType(ConstructorType(ctor, ctor.outerDecl))

        return Insight.SKIP
    }

    override fun visit(method: MethodDecl): Insight {
        method.params?.visit(this)
        method.retTypeRef?.visit(this)
        overloadManager += method

        val retType = method.retTypeRef?.rootType
        val methodType = MethodType(method, method.outerDecl, retType)
        method.specifyType(methodType)

        return Insight.SKIP
    }
}