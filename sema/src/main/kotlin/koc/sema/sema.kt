package koc.sema

import koc.ast.Node
import koc.ast.visitor.Visitor
import koc.core.Diagnostics
import koc.sema.impl.ClassCollector
import koc.sema.impl.ClassMemberReferenceCollector
import koc.sema.impl.OverloadManager
import koc.sema.impl.OverloadValidatorVisitor
import koc.sema.impl.ReferenceResolver
import koc.sema.impl.ScopeManager
import koc.sema.impl.SuperTypeResolver
import koc.sema.impl.TypeChecker
import koc.sema.impl.VariableAccessOrderChecker

fun performSema(nodes: List<Node>, typeManager: TypeManager, diag: Diagnostics) {
    semaStages(typeManager, diag).forEach { stage -> stage(nodes) }
}

fun performSemaStage(nodes: List<Node>, stage: Visitor<*>, typeManager: TypeManager) {
    typeManager.builtInDeclarations.forEach { node ->
        node.visit(stage)
        stage.reset()
    }

    nodes.forEach { node ->
        node.visit(stage)
        stage.reset()
    }
}

fun semaStages(
    typeManager: TypeManager, diag: Diagnostics = Diagnostics()
): List<(List<Node>) -> Unit> = semaVisitors(typeManager, diag).map { stage ->
    { nodes ->
        performSemaStage(nodes, stage, typeManager)
    }
}

fun semaVisitors(typeManager: TypeManager, diag: Diagnostics = Diagnostics()): List<Visitor<*>> {
    val scopeManager = ScopeManager(diag)
    val overloadManager = OverloadManager(diag)
    return listOf(
        ClassCollector(typeManager, diag, scopeManager),
        SuperTypeResolver(typeManager, diag),
        ClassMemberReferenceCollector(scopeManager),
        OverloadValidatorVisitor(typeManager, diag, scopeManager, overloadManager),
        VariableAccessOrderChecker(typeManager, diag, scopeManager),
        ReferenceResolver(typeManager, diag, scopeManager, overloadManager),
        TypeChecker(typeManager, diag, scopeManager, overloadManager)
    )
}