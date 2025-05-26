package koc.sema.impl

import koc.ast.BooleanLiteral
import koc.ast.CallExpr
import koc.ast.ClassDecl
import koc.ast.ClassRefType
import koc.ast.ConstructorDecl
import koc.ast.ConstructorRefType
import koc.ast.EntityRefType
import koc.ast.FieldDecl
import koc.ast.FieldType
import koc.ast.IntegerLiteral
import koc.ast.MemberAccessExpr
import koc.ast.MethodDecl
import koc.ast.MethodRefType
import koc.ast.RealLiteral
import koc.ast.RefExpr
import koc.ast.TypeParam
import koc.ast.VarDecl
import koc.ast.VarType
import koc.ast.visitor.AbstractVoidInsightVisitor
import koc.ast.visitor.Insight
import koc.core.Diagnostics
import koc.lex.asWindow
import koc.lex.diag
import koc.parser.ast.Attribute
import koc.parser.impl.ParseScope
import koc.sema.TypeManager
import koc.sema.diag.MemberAccessOnClass
import koc.sema.diag.MethodReferenceWithoutCall
import koc.sema.diag.NoSuitableConstructorCandidate
import koc.sema.diag.NoSuitableMethodCandidate
import koc.sema.diag.NonReturningCallInExpr
import koc.sema.diag.ThisOutOfContext
import koc.sema.diag.UndefinedReference

class ReferenceResolver(
    private val typeManager: TypeManager,
    private val diag: Diagnostics,
    private val scopeManager: ScopeManager,
    private val overloadManager: OverloadManager
) : AbstractVoidInsightVisitor() {
    private var context: ClassDecl? = null

    override fun visit(lit: BooleanLiteral): Insight {
        if (!lit.isTypeKnown) lit.specifyType(typeManager.boolType)
        return Insight.SKIP
    }

    override fun visit(lit: IntegerLiteral): Insight {
        if (!lit.isTypeKnown) lit.specifyType(typeManager.intType)
        return Insight.SKIP
    }

    override fun visit(lit: RealLiteral): Insight {
        if (!lit.isTypeKnown) lit.specifyType(typeManager.realType)
        return Insight.SKIP
    }

    override fun visit(field: FieldDecl): Insight {
        field.specifyType(FieldType(field, field.outerDecl))
        field.varDecl.initializer.visit(this)
        postvisit(field.varDecl, Insight.SKIP)
        return Insight.SKIP
    }

    override fun visit(vardecl: VarDecl): Insight {
        scopeManager += vardecl
        return super.visit(vardecl)
    }

    override fun postvisit(vardecl: VarDecl, res: Insight) {
        if (vardecl.initializer.isBroken) {
            vardecl.enable(Attribute.BROKEN)
            vardecl.specifyType(VarType(vardecl, typeManager.invalidType))
            return
        }
        if (vardecl.initializer.isTypeKnown) {
            if (vardecl.initializer.rootType.isUnit) {
                diag.diag(NonReturningCallInExpr(vardecl.initializer), vardecl.initializer.window)
                vardecl.initializer.enable(Attribute.BROKEN)
                vardecl.enable(Attribute.BROKEN)
                vardecl.specifyType(VarType(vardecl, typeManager.invalidType))
            } else vardecl.specifyType(VarType(vardecl, vardecl.initializer.rootType))
        }
    }

    override fun visit(method: MethodDecl): Insight {
        return super.visit(method)
    }

    override fun visit(ctor: ConstructorDecl): Insight {
        return super.visit(ctor)
    }

    override fun visit(node: TypeParam): Insight {
        scopeManager += node
        return super.visit(node)
    }

    private fun visit(expr: CallExpr, scope: ParseScope): Insight {
        if (expr.ref.ref != null) return Insight.SKIP

        expr.args.forEach { it.visit(this) }

        if (scopeManager.isMethodDefined(expr.ref.identifier, scope)) {
            val randomMethod = scopeManager.getMethodDecl(expr.ref.identifier, scope) as MethodDecl

            val method = overloadManager.getSuitable(randomMethod.outerDecl.identifier, randomMethod.identifier, expr.argumentTypes)
            if (method == null) {
                diag.diag(NoSuitableMethodCandidate(expr), expr.window)
                expr.enable(Attribute.BROKEN)
                expr.ref.enable(Attribute.BROKEN)
                expr.ref.specifyRef(typeManager.invalidDecl)
                expr.ref.specifyType(typeManager.invalidType)
                expr.specifyType(typeManager.invalidType)
                return Insight.SKIP
            } else {
                expr.ref.specifyRef(method)
                expr.ref.specifyType(MethodRefType(method))
                if (method.type.isVoid) {
//                    diag.diag(NonReturningCallInExpr(expr), expr.window)
//                    expr.enable(Attribute.BROKEN)
                    expr.specifyType(typeManager.unitType)
//                    expr.specifyType(typeManager.invalidType)
                } else {
                    expr.specifyType(method.type.retType!!)
                }
            }

        } else if (scopeManager.isDefined(expr.ref.identifier, scope)) {
            val classDecl = scopeManager.getDecl(expr.ref.identifier, scope) as ClassDecl
            expr.enable(Attribute.CONSTRUCTOR_CALL)

            val ctor = overloadManager.getSuitable(classDecl.identifier, expr.argumentTypes)
            if (ctor == null) {
                diag.diag(NoSuitableConstructorCandidate(expr, classDecl), expr.window)
                expr.enable(Attribute.BROKEN)
                expr.ref.enable(Attribute.BROKEN)
                expr.ref.specifyRef(typeManager.invalidDecl)
                expr.ref.specifyType(typeManager.invalidType)
                expr.specifyType(typeManager.invalidType)
                return Insight.SKIP
            } else {
                expr.ref.specifyRef(ctor)
                expr.ref.specifyType(ConstructorRefType(ctor))
                expr.specifyType(ctor.outerDecl.type)
            }
        } else {
            expr.ref.specifyRef(typeManager.invalidDecl)
            diag.diag(UndefinedReference(expr.ref), expr.ref.window)
            expr.ref.enable(Attribute.BROKEN)
            expr.enable(Attribute.BROKEN)
            expr.specifyType(typeManager.invalidType)
        }

        return Insight.SKIP
    }

    override fun visit(expr: CallExpr): Insight {
        return visit(expr, expr.scope)
    }

    override fun visit(expr: RefExpr): Insight {
        return visit(expr, expr.scope)
    }

    private fun visit(expr: RefExpr, scope: ParseScope): Insight {
        if (expr.ref != null) return Insight.SKIP

        if (expr.isThis) {
            if (scope != expr.scope) {
                error("'this' is in bad context")
            } else if (context != null) {
                expr.specifyRef(context!!)
                expr.specifyType(EntityRefType(context!!))
            } else {
                diag.diag(ThisOutOfContext(expr), expr.window)
                expr.specifyRef(typeManager.invalidDecl)
                expr.specifyType(typeManager.invalidType)
                expr.enable(Attribute.BROKEN)
            }
        } else {
            if (scopeManager.isDefined(expr.identifier, scope)) {
                val decl = scopeManager.getDecl(expr.identifier, scope)
                expr.specifyRef(decl)
                val refType = if (decl.isBroken) {
                    expr.enable(Attribute.BROKEN)
                    typeManager.invalidType
                } else if (decl is ClassDecl) ClassRefType(decl) else EntityRefType(decl)
                expr.specifyType(refType)
            } else if (scopeManager.isMethodDefined(expr.identifier, scope)) {
                diag.diag(MethodReferenceWithoutCall(expr), expr.window)
                expr.enable(Attribute.BROKEN)
            } else {
                expr.specifyRef(typeManager.invalidDecl)
                expr.specifyType(typeManager.invalidType)
                diag.diag(UndefinedReference(expr), expr.window)
                expr.enable(Attribute.BROKEN)
            }
        }

        if (!expr.isBroken && expr.ref is FieldDecl && !expr.ref!!.isTypeKnown) {
            val field = expr.ref as FieldDecl
            field.varDecl.initializer.visit(this)
            field.varDecl.specifyType(VarType(field.varDecl, field.varDecl.initializer.rootType))
            field.specifyType(FieldType(field, field.outerDecl))
        } else if (!expr.isBroken && expr.ref is VarDecl && !expr.ref!!.isTypeKnown) {
            val vardecl = expr.ref as VarDecl

            vardecl.initializer.visit(this)
            vardecl.specifyType(VarType(vardecl, vardecl.initializer.rootType))
        }

        return Insight.CONTINUE
    }

    private fun visit(access: MemberAccessExpr, scope: ParseScope = access.scope): Insight {
        when (access.left) {
            is RefExpr -> visit(access.left as RefExpr, scope).also {
                if (access.left.isBroken) {
                    access.enable(Attribute.BROKEN)
                    access.member.enable(Attribute.BROKEN)
                    return Insight.SKIP
                }
                if ((access.left as RefExpr).type.isClass) {
                    diag.diag(MemberAccessOnClass(access), access.window)
                    access.enable(Attribute.BROKEN)
                    return Insight.SKIP
                }
            }
            is CallExpr -> visit(access.left as CallExpr, scope)
            else -> access.left.visit(this)
        }

        if (access.left.isBroken || access.left.rootType.isUnit) {
            if (access.left.rootType.isUnit) diag.diag(NonReturningCallInExpr(access.left), access.left.window)
            access.member.enable(Attribute.BROKEN)
            access.specifyType(typeManager.invalidType)
            access.enable(Attribute.BROKEN)
            return Insight.SKIP
        }

        // require(member is CallExpr || member is RefExpr || member is MemberAccessExpr)
        val leftExprScope = access.left.rootType.classDecl.body.scope.inside()
        when (access.member) {
            is RefExpr -> visit(access.member as RefExpr, leftExprScope)
            is CallExpr -> visit(access.member as CallExpr, leftExprScope)
            is MemberAccessExpr -> visit(access.member as MemberAccessExpr, leftExprScope)
            else -> error("unreachable state; member access right could be: ref, call, member access")
        }

        access.specifyType(access.member.rootType)
        return Insight.SKIP
    }

    override fun visit(access: MemberAccessExpr): Insight {
        return visit(access, access.scope)
    }

    override fun previsit(classDecl: ClassDecl) {
        context = classDecl
    }

    override fun postvisit(classDecl: ClassDecl, res: Insight) {
        context = null
    }
}