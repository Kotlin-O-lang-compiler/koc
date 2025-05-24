package koc.sema.impl

import koc.ast.Decl
import koc.ast.MethodDecl
import koc.core.Diagnostics
import koc.lex.diag
import koc.parser.ast.Attribute
import koc.parser.ast.Identifier
import koc.parser.impl.ParseScope
import koc.sema.TypeManager
import koc.sema.diag.DeclRedefinition

class ScopeManager(val diag: Diagnostics) {
    private val storageDecl = hashMapOf<ParseScope, ArrayList<Decl>>()
    private val storageId = hashMapOf<ParseScope, ArrayList<Identifier>>()

    operator fun plusAssign(decl: Decl) {
        if (!decl.isBuiltIn && decl !is MethodDecl && decl.identifier.isDefinedInScope(decl.scope)) {
            diag.diag(DeclRedefinition(decl, decl.scope.getDecl(decl.identifier)), decl.identifierToken)
            decl.enable(Attribute.BROKEN)
        }
        storageDecl.getOrPut(decl.scope) { arrayListOf() } += decl
        storageId.getOrPut(decl.scope) { arrayListOf() } += decl.identifier
    }

    private fun ParseScope.getDecl(identifier: Identifier): Decl {
        val scope = lastOrNull { it in storageId && identifier in storageId[it]!! }
        val idx = storageId[scope]!!.indexOf(identifier)
        return storageDecl[scope]!![idx]
    }

    private fun Identifier.isDefinedInScope(scope: ParseScope): Boolean {
        return scope.any { scope ->
            scope in storageId && this in storageId[scope]!!
        }
    }

    fun initializeBuiltIn(typeManager: TypeManager) {
        this += typeManager.invalidDecl
        this += typeManager.classDecl
        this += typeManager.anyValueDecl
        this += typeManager.anyRefDecl
        this += typeManager.boolDecl
        this += typeManager.intDecl
        this += typeManager.realDecl
    }

    fun isDefined(decl: Decl, scope: ParseScope): Boolean = decl.identifier.isDefinedInScope(scope)
    fun isDefined(decl: Identifier, scope: ParseScope): Boolean = decl.isDefinedInScope(scope)
    fun getDecl(identifier: Identifier, scope: ParseScope): Decl = scope.getDecl(identifier)
}
