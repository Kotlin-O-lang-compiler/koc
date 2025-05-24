package koc.sema

import koc.ast.Node
import koc.ast.visitor.Visitor
import koc.sema.impl.ClassCollector
import koc.core.Diagnostics
import koc.sema.impl.OverloadManager
import koc.sema.impl.OverloadValidatorVisitor
import koc.sema.impl.ReferenceResolver
import koc.sema.impl.ScopeManager
import koc.sema.impl.SuperTypeResolver

fun performSema(nodes: List<Node>, typeManager: TypeManager, diag: Diagnostics) {
    semaStages(typeManager, diag).forEach { stage -> stage(nodes) }
}

fun performSemaStage(nodes: List<Node>, stage: Visitor<*>) {
    nodes.forEach { node ->
        node.visit(stage)
        stage.reset()
    }
}

fun semaStages(
    typeManager: TypeManager, diag: Diagnostics = Diagnostics()
): List<(List<Node>) -> Unit> = semaVisitors(typeManager, diag).map { stage ->
    { nodes -> performSemaStage(nodes, stage) }
}

fun semaVisitors(typeManager: TypeManager, diag: Diagnostics = Diagnostics()): List<Visitor<*>> {
    val scopeManager = ScopeManager(diag)
    val overloadManager = OverloadManager(diag)
    scopeManager.initializeBuiltIn(typeManager)
    return listOf(
        ClassCollector(typeManager, diag, scopeManager),
        ReferenceResolver(typeManager, diag, scopeManager),
        SuperTypeResolver(typeManager, diag),
        OverloadValidatorVisitor(typeManager, diag, overloadManager),
    )
}