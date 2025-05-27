package koc.sema.impl

import koc.ast.Decl
import koc.ast.MethodDecl
import koc.core.Diagnostics
import koc.lex.diag
import koc.parser.ast.Attribute
import koc.parser.ast.Identifier
import koc.parser.impl.ParseScope
import koc.sema.diag.DeclRedefinition

class ScopeManager(val diag: Diagnostics) {
    private val storageDecl = hashMapOf<ParseScope, ArrayList<Decl>>()
    private val storageId = hashMapOf<ParseScope, ArrayList<Identifier>>()

    private val methodDecl = hashMapOf<ParseScope, ArrayList<MethodDecl>>()
    private val methodId = hashMapOf<ParseScope, ArrayList<Identifier>>()

    operator fun plusAssign(decl: Decl) {
        if (!decl.isBuiltIn && decl !is MethodDecl && decl.identifier.isDefinedInScope(decl.scope)) {
            diag.diag(DeclRedefinition(decl, decl.scope.getDecl(decl.identifier)), decl.identifierToken)
            decl.enable(Attribute.BROKEN)
        }

        if (decl !is MethodDecl) {
            storageDecl.getOrPut(decl.scope) { arrayListOf() } += decl
            storageId.getOrPut(decl.scope) { arrayListOf() } += decl.identifier
        } else {
            methodDecl.getOrPut(decl.scope) { arrayListOf() } += decl
            methodId.getOrPut(decl.scope) { arrayListOf() } += decl.identifier
        }
    }

    private fun ParseScope.getDecl(identifier: Identifier): Decl {
        val scope = lastOrNull { it in storageId && identifier in storageId[it]!! }
        val idx = storageId[scope]!!.indexOf(identifier)
        return storageDecl[scope]!![idx]
    }

    private fun ParseScope.getMethodDecl(identifier: Identifier): Decl {
        val scope = lastOrNull { it in methodId && identifier in methodId[it]!! }
        val idx = methodId[scope]!!.indexOf(identifier)
        return methodDecl[scope]!![idx]
    }

    private fun Identifier.isDefinedInScope(scope: ParseScope): Boolean {
        return scope.any { scope ->
            scope in storageId && this in storageId[scope]!!
        }
    }

    private fun Identifier.isMethodDefinedInScope(scope: ParseScope): Boolean {
        return scope.any { scope ->
            scope in methodId && this in methodId[scope]!!
        }
    }

    fun isDefined(decl: Decl, scope: ParseScope): Boolean = decl.identifier.isDefinedInScope(scope)
    fun isMethodDefined(decl: Decl, scope: ParseScope): Boolean = decl.identifier.isMethodDefinedInScope(scope)
    fun isDefined(decl: Identifier, scope: ParseScope): Boolean = decl.isDefinedInScope(scope)
    fun isMethodDefined(decl: Identifier, scope: ParseScope): Boolean = decl.isMethodDefinedInScope(scope)
    fun getDecl(identifier: Identifier, scope: ParseScope): Decl = scope.getDecl(identifier)
    fun getMethodDecl(identifier: Identifier, scope: ParseScope): Decl = scope.getMethodDecl(identifier)
}
